package com.riprod.hexcode.core.common.hexes.codec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.riprod.hexcode.core.common.hexes.component.Hex;

public class DecodeResult {

    @Nullable
    private final Hex hex;
    private final List<DecodeIssue> issues;

    public DecodeResult(@Nullable Hex hex, List<DecodeIssue> issues) {
        this.hex = hex;
        this.issues = Collections.unmodifiableList(new ArrayList<>(issues));
    }

    @Nullable
    public Hex getHex() {
        return hex;
    }

    public List<DecodeIssue> getIssues() {
        return issues;
    }

    public boolean isOk() {
        return hex != null && issues.stream()
                .noneMatch(i -> i.getSeverity() == DecodeIssue.Severity.ERROR);
    }

    public static DecodeResult error(String message) {
        return new DecodeResult(null, List.of(new DecodeIssue(message, DecodeIssue.Severity.ERROR)));
    }
}
