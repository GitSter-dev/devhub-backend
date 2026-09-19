package com.application.devhub.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

public record ReceiptsRequest(
        @Schema(description = "Every message up to this seq reached the device", example = "42")
        @PositiveOrZero
        long deliveredSeq,

        @Schema(description = "Every message up to this seq was seen on screen", example = "40")
        @PositiveOrZero
        long readSeq) {
}
