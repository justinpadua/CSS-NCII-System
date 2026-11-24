package com.cssncii.dto;

import jakarta.validation.constraints.Size;

public record StudentNotesRequest(@Size(max = 65535) String adminNotes) {
}
