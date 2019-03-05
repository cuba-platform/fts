/*
 * Copyright (c) 2008-2019 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.fts.core.sys;

import org.apache.lucene.index.*;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/** This {@link MergePolicy} is used for upgrading all existing segments of
 * an index when calling {@link IndexWriter#forceMerge(int)}.
 * This allows for an as-cheap-as possible upgrade of an older index by only upgrading segments that
 * are created by previous Lucene versions. forceMerge does no longer really merge;
 * it is just used to &quot;forceMerge&quot; older segment versions away.
 * <p>For a fully customizeable upgrade, you can use this like any other {@code MergePolicy}
 * and call {@link IndexWriter#forceMerge(int)}:
 * <pre class="prettyprint lang-java">
 *  IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_XX, new KeywordAnalyzer());
 *  iwc.setMergePolicy(new LiveUpgradeMergePolicy(iwc.getMergePolicy()));
 *  IndexWriter w = new IndexWriter(dir, iwc);
 *  w.forceMerge(1);
 *  w.close();
 * </pre>
 * <p><b>Warning:</b> This merge policy may reorder documents if the index was partially
 * upgraded before calling forceMerge (e.g., documents were added). If your application relies
 * on &quot;monotonicity&quot; of doc IDs (which means that the order in which the documents
 * were added to the index is preserved), do a forceMerge(1) instead. Please note, the
 * delegate {@code MergePolicy} may also reorder documents.
  */
//https://issues.apache.org/jira/browse/LUCENE-7671?page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel&focusedCommentId=15952832#comment-15952832
//https://github.com/apache/lucene-solr/pull/151/files
public class LiveUpgradeMergePolicy extends FilterMergePolicy {
    // True if the next merge request should do segment upgrades:
    private volatile boolean upgradeInProgress;

    private static final Logger log = LoggerFactory.getLogger(LiveUpgradeMergePolicy.class);

    /**
     * Creates a new merge policy instance.
     *
     * @param in the wrapped {@link MergePolicy}
     */
    public LiveUpgradeMergePolicy(MergePolicy in) {
        super(in);
    }

    public void setUpgradeInProgress(boolean upgradeInProgress) {
        this.upgradeInProgress = upgradeInProgress;
    }

    /**
     * Returns if the given segment should be upgraded. The default implementation
     * will return {@code !Version.LATEST.equals(si.getVersion())},
     * so all segments created with a different version number than this Lucene version will
     * get upgraded.
     */
    protected boolean shouldUpgradeSegment(SegmentCommitInfo si) {
        return !Version.LATEST.equals(si.info.getVersion());
    }

    @Override
    public MergeSpecification findForcedMerges(SegmentInfos segmentInfos, int maxSegmentCount,
                                               Map<SegmentCommitInfo,Boolean> segmentsToMerge, MergeContext mergeContext) throws IOException {
        MergeSpecification spec = in.findForcedMerges(segmentInfos, maxSegmentCount, segmentsToMerge, mergeContext);

        if (upgradeInProgress) {
            try {
                // first find all old segments
                final Map<SegmentCommitInfo, Boolean> oldSegments = new HashMap<>();
                for (final SegmentCommitInfo si : segmentInfos) {
                    final Boolean v = segmentsToMerge.get(si);
                    if (v != null && shouldUpgradeSegment(si)) {
                        oldSegments.put(si, v);
                    }
                }

                log.trace("findForcedMerges: segmentsToUpgrade={}", oldSegments);

                if (oldSegments.isEmpty()) {
                    return spec;
                }

                if (spec != null) {
                    // remove all segments that are in merge specification from oldSegments,
                    // the resulting set contains all segments that are left over
                    // and will be merged to one additional segment:
                    for (final OneMerge om : spec.merges) {
                        oldSegments.keySet().removeAll(om.segments);
                    }
                }

                if (!oldSegments.isEmpty()) {
                    log.trace("findForcedMerges: {} does not want to merge all old segments, merge remaining ones into new segment: {}",
                            in.getClass().getSimpleName(), oldSegments);

                    if (spec == null) {
                        spec = new MergeSpecification();
                    }

                    for (final SegmentCommitInfo si : segmentInfos) {
                        if (oldSegments.containsKey(si)) {
                            // Add a merge of only the upgrading segment to the spec
                            // We don't want to merge, just upgrade
                            spec.add(new OneMerge(Collections.singletonList(si)));
                        }
                    }
                }

                return spec;

            } finally {
                upgradeInProgress = false;
            }
        } else {
            return spec;
        }
    }
}
