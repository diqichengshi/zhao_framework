package org.springframework.beans.factory.parsing;

import org.springframework.core.io.Resource;

public interface SourceExtractor {

    /**
     * Extract the source metadata from the candidate object supplied
     * by the configuration parser.
     *
     * @param sourceCandidate  the original source metadata (never {@code null})
     * @param definingResource the resource that defines the given source object
     *                         (may be {@code null})
     * @return the source metadata object to store (may be {@code null})
     */
    Object extractSource(Object sourceCandidate, Resource definingResource);
}
