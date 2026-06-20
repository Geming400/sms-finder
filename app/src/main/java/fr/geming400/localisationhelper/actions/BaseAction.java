package fr.geming400.localisationhelper.actions;

import android.content.BroadcastReceiver;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import fr.geming400.localisationhelper.utils.Utils;

/**
 * Represents an action that is able to send a payload to another phone and parse its response back to {@link P}.
 * <p>
 * The sent sms is and always will be in the format: {@code "payloadType/name:content"}, however if {@code content} is {@code null}
 * because we're sending an {@linkplain PayloadType#INSTRUCTION instruction sms}, the format will be instead {@code "payloadType/name"}
 */
public abstract class BaseAction<T, P> {
    private final String name;

    public BaseAction(String name) {
        final String regexPattern = "[a-zA-Z0-9-_]*";
        if (!name.matches(regexPattern))
            throw new RuntimeException("Action's name '" + name + "' doesn't respect regex " + regexPattern);

        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Gets the name of this with the password hashed using {@code SHA-256}
     * @param password the password
     * @return the action's name in the format {@code "hashed pass/name"}
     * @implNote if the {@code SHA-256} algorithm doesn't exist, only the {@linkplain #getName() name} will be returned
     */
    public String getName(String password) {
        try {
            return this.digestPasswordAsSha256(password) + "/" + this.getName();
        } catch (NoSuchAlgorithmException e) {
            return this.getName();
        }
    }

    protected String digestPasswordAsSha256(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return Arrays.toString(md.digest(password.getBytes(StandardCharsets.US_ASCII)));
    }

    /**
     * Sends an "instruction" sms, instructing the {@linkplain Stage#RECEIVE_OTHER_PHONE other phone} to execute this action and
     * send back the serialized action's output
     * @param context the android app's {@link Context}
     * @param sender the sender's phone number
     * @implSpec this should call {@link #smsSenderHelper(Context, String, HasPayloadType, String)} when sending a sms,
     * with the current {@linkplain PayloadType payload type} being {@link PayloadType#INSTRUCTION PayloadType#INSTRUCTION}
     * @see PayloadType#INSTRUCTION
     */
    public void sendInstructionSMS(@NonNull Context context, @NonNull String sender) {
        this.smsSenderHelper(context, sender, PayloadType.INSTRUCTION, null);
    }

    /**
     * Sends a "data" sms, instructing the {@linkplain Stage#RECEIVE_HOST host phone} to update (for example) its {@linkplain androidx.datastore.core.DataStore datastore} and ui
     * by {@linkplain Action#parse(String) parsing} the raw sms content
     * @param context the android app's {@link Context}
     * @param sender the sender's phone number
     * @implSpec this should call {@link #smsSenderHelper(Context, String, HasPayloadType, String)} when sending a sms,
     * with the current {@linkplain PayloadType payload type} being {@link PayloadType#DATA PayloadType#DATA}
     * @see PayloadType#DATA
     */
    public abstract void sendDataSMS(@NonNull Context context, @NonNull String sender);

    // TODO: Encode content
    protected final void smsSenderHelper(@NonNull Context context, @NonNull String sender, @NonNull HasPayloadType payloadType, @Nullable String content) {
        String prefix = payloadType.getPayloadType().getPayloadName() + "/";
        String suffix = content == null ? "" : ":" + content;
        Utils.sendSMS(context, sender, prefix + this.getName() + suffix);
    }

    /**
     * Checks if this action has no return type
     * @return if this action has no return type
     * @see VoidAction
     */
    public boolean isVoid() {
        return this instanceof VoidAction;
    }

    /**
     * Execute this action and returns its result.
     * @param context the android app's {@link Context}
     * @return the result of this action
     * @apiNote this should get called at the {@link Stage#RECEIVE_OTHER_PHONE RECEIVE_OTHER_PHONE} stage
     */
    @Nullable
    public abstract T execute(Context context);

    /**
     * Called whenever this phone receives a {@code sms} linked to this action
     * @param context the android app's {@link Context}
     * @param sender the sender's phone number
     * @param pendingResult the {@link BroadcastReceiver.PendingResult PendingResult} used by the {@link BroadcastReceiver}.
     *                      Though, it should <b>not</b> be {@linkplain BroadcastReceiver.PendingResult#finish() finished} or marked as {@linkplain BroadcastReceiver.PendingResult#goAsync() async} !
     * @param stage the stage from which the sms got received. Cannot be {@link Stage#SEND_INSTRUCTIONS}
     * @param rawContent the rawContent of the action. This <b>doesn't contain the payload type and action name</b>.
     *                   When the stage's payload type is an {@linkplain PayloadType#INSTRUCTION instruction} one, it is always empty. (aka {@code ""})
     */
    public abstract void onReceive(@NonNull Context context, @NonNull String sender, @NonNull BroadcastReceiver.PendingResult pendingResult, @NonNull Stage stage, @NonNull String rawContent);

    @NonNull
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "name='" + this.name + '\'' +
                "isVoid='" + this.isVoid() + '\'' +
                '}';
    }

    public enum Stage implements HasPayloadType {
        /// This stage is when the <b>host phone</b> (the one sending instructions to the {@linkplain #RECEIVE_OTHER_PHONE other phone})
        /// is ready to send data to the other phone.
        /// <p>
        /// This stage is triggered whenever this phone via the ui is told to send an "instruction" payload.
        SEND_INSTRUCTIONS(PayloadType.INSTRUCTION, false),
        /// This stage is when the <b>other phone</b> (the one sending data back to the {@linkplain #RECEIVE_HOST host phone})
        /// received instructions and is then supposed to respond.
        /// <p>
        /// This stage is triggered whenever this phone receives an "instruction" payload.
        RECEIVE_OTHER_PHONE(PayloadType.INSTRUCTION, true),
        /// This stage is when the <b>host phone</b> (the one sending instructions to the {@linkplain #RECEIVE_OTHER_PHONE other phone})
        /// received the data from the other phone.
        /// <p>
        /// This stage is triggered whenever this phone receives a "data" payload.
        RECEIVE_HOST(PayloadType.DATA, true);

        private final PayloadType payloadType;
        private final boolean isReceiver;

        Stage(PayloadType payloadType, boolean isReceiver) {
            this.payloadType = payloadType;
            this.isReceiver = isReceiver;
        }

        /**
         * Gets this stage's payload type, e.g. {@link PayloadType#DATA PayloadType#DATA} or {@link PayloadType#INSTRUCTION PayloadType#INSTRUCTION}
         * <p>
         * For example, {@link #RECEIVE_HOST} is <b>triggered</b> via a {@code "data"} payload, so it will return {@link PayloadType#DATA PayloadType#DATA},
         * however {@link #SEND_INSTRUCTIONS} <b>sends</b> a {@code "instruction"} payload, so it will return {@link PayloadType#INSTRUCTION PayloadType#INSTRUCTION}.
         * @return this stage's payload name
         */
        @NonNull
        public PayloadType getPayloadType() {
            return this.payloadType;
        }

        /**
         * Gets if this stage is triggered by a {@link BroadcastReceiver}
         * @return if this stage is triggered by a {@link BroadcastReceiver}
         */
        public boolean isReceiver() {
            return this.isReceiver;
        }

        @Nullable
        public static Stage fromPayloadString(String payloadName) {
            PayloadType payloadType = PayloadType.fromPayloadName(payloadName);
            return payloadType == null
                    ? null
                    : fromPayloadType(payloadType);
        }

        @NonNull
        public static Stage fromPayloadType(PayloadType payloadType) {
            if (payloadType == PayloadType.DATA) {
                return Stage.RECEIVE_HOST;
            } else {
                return Stage.RECEIVE_OTHER_PHONE;
            }
        }
    }
}
