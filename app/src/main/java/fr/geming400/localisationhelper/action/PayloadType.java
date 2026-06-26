package fr.geming400.localisationhelper.action;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public enum PayloadType implements HasPayloadType {
    INSTRUCTION("instruction"),
    DATA("data");

    private final String payloadName;

    PayloadType(String payloadName) {
        this.payloadName = payloadName;
    }

    /**
     * Gets this stage's payload name, e.g. {@code "data"} or {@code "instruction"}.
     * <p>
     * For example, {@link BaseAction.Stage#RECEIVE_HOST Stage.RECEIVE_HOST} is <b>triggered</b> via a {@code "data"} payload, so it will return {@code "data"},
     * however {@link BaseAction.Stage#SEND_INSTRUCTIONS Stage.SEND_INSTRUCTIONS} <b>sends</b> a {@code "instruction"} payload, so it will return {@code "instruction"},
     * @return this stage's payload name.
     */
    public String getPayloadName() {
        return this.payloadName;
    }

    /**
     * Returns {@linkplain PayloadType this}
     * @return {@linkplain PayloadType this}
     */
    @NonNull
    @Override
    public PayloadType getPayloadType() {
        return this;
    }

    @Nullable
    public static PayloadType fromPayloadName(String payloadName) {
        for (PayloadType type : values()) {
            if (payloadName.equals(type.payloadName))
                return type;
        }

        return null;
    }
}
