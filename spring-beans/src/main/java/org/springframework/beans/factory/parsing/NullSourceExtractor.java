package org.springframework.beans.factory.parsing;

import org.springframework.core.io.Resource;

public class NullSourceExtractor implements SourceExtractor {

    /**
     * This implementation simply returns {@code null} for any input.
     */
    @Override
    public Object extractSource(Object sourceCandidate, Resource definitionResource) {
        return null;
    }

}

