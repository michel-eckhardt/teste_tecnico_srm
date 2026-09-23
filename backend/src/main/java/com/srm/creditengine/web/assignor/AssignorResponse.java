package com.srm.creditengine.web.assignor;

import com.srm.creditengine.domain.assignor.Assignor;
import java.time.Instant;
import java.util.UUID;

public record AssignorResponse(UUID id, String name, String document, Instant createdAt) {

    static AssignorResponse from(Assignor assignor) {
        return new AssignorResponse(
                assignor.getId(), assignor.getName(), assignor.getDocument(), assignor.getCreatedAt());
    }
}
