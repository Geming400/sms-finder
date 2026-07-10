package fr.geming400.localisationhelper.action;

import android.content.BroadcastReceiver;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Base64;
import java.util.Set;

import fr.geming400.localisationhelper.datastore.TrackingData;
import fr.geming400.localisationhelper.ui.settings.Setting;
import fr.geming400.localisationhelper.utils.SmsCryptography;
import fr.geming400.localisationhelper.utils.Utils;

/**
 * Represents an action that is able to send a payload to another phone and parse its response back to {@link P}.
 * <p>
 * The sent sms is and always will be in the format: {@code "payloadType/name:content"}, however if {@code content} is {@code null}
 * because we're sending an {@linkplain PayloadType#INSTRUCTION instruction sms}, the format will be instead {@code "payloadType/name"}
 */
public abstract class BaseAction<T, P> {
    public static final String DELIMITER = ";";

    private final String name;
    private final Set<Setting.BooleanSetting> dependentSettings;

    public BaseAction(String name) {
        this.name = validateName(name);
        this.dependentSettings = Set.of();
    }

    public BaseAction(String name, Setting.BooleanSetting... dependentSettings) {
        this.name = validateName(name);
        this.dependentSettings = Set.copyOf(Arrays.asList(dependentSettings));
    }

    public String getName() {
        return this.name;
    }

    @Unmodifiable
    public Set<Setting.BooleanSetting> getDependentSettings() {
        return Set.copyOf(this.dependentSettings);
    }

    /**
     * Sends an "instruction" sms, instructing the {@linkplain Stage#RECEIVE_OTHER_PHONE other phone} to execute this action and
     * send back the serialized action's output
     * @param context the android app's {@link Context}
     * @param sender the sender's phone number
     * @param privateKey the private key used to encode the content
     * @implSpec this should call {@link #smsSenderHelper(Context, String, HasPayloadType, String, String)} (Context, String)} when sending a sms,
     * with the current {@linkplain PayloadType payload type} being {@link PayloadType#INSTRUCTION PayloadType#INSTRUCTION}
     * @see PayloadType#INSTRUCTION
     */
    public void sendInstructionSMS(@NonNull Context context, @NonNull String sender, @NonNull String privateKey) {
        this.smsSenderHelper(context, sender, PayloadType.INSTRUCTION, null, privateKey);
    }

    /**
     * Sends a "data" sms, instructing the {@linkplain Stage#RECEIVE_HOST host phone} to update (for example) its {@linkplain androidx.datastore.core.DataStore datastore} and ui
     * by {@linkplain Action#parse(String) parsing} the raw sms content
     * @param context the android app's {@link Context}
     * @param sender the sender's phone number
     * @param privateKey the private key used to encode the content
     * @implSpec this should call {@link #smsSenderHelper(Context, String, HasPayloadType, String, String)} when sending a sms,
     * with the current {@linkplain PayloadType payload type} being {@link PayloadType#DATA PayloadType#DATA}
     * @see PayloadType#DATA
     */
    public abstract void sendDataSMS(@NonNull Context context, @NonNull String sender, @NonNull String privateKey);

    protected final void smsSenderHelper(
            @NonNull Context context,
            @NonNull String sender,
            @NonNull HasPayloadType payloadType,
            @Nullable String content,
            @NonNull String privateKey
    ) {
        Base64.Encoder encoder = Base64.getEncoder();

        String prefix = payloadType.getPayloadType().getPayloadName() + DELIMITER;
        String suffix = content == null
                ? ""
                : ":" + content;

        // Examples:
        // instruction/<location>
        // data;[iv];<location:24.49874:-14.6391>
        //
        // What's surrounded in <> is encrypted and then base64-ed
        // What's surrounded in [] is simply base64-ed

        String body = this.getName() + suffix;
        SmsCryptography.EncryptedContent encryptedContent = SmsCryptography.encryptContent(body.getBytes(), privateKey, SmsCryptography.STATIC_SALT);
        Utils.sendSMS(
                context,
                sender,
                prefix + encoder.encodeToString(encryptedContent.iv) + DELIMITER + encoder.encodeToString(encryptedContent.encryptedData)
        );
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
    public abstract T execute(@NonNull Context context);

    /**
     * Called whenever this phone receives a {@code sms} linked to this action
     * @param context the android app's {@link Context}
     * @param sender the sender's phone number
     * @param pendingResult the {@link BroadcastReceiver.PendingResult PendingResult} used by the {@link BroadcastReceiver}.
     *                      Though, it should <b>not</b> be {@linkplain BroadcastReceiver.PendingResult#finish() finished} or marked as {@linkplain BroadcastReceiver.PendingResult#goAsync() async} !
     * @param stage the stage from which the sms got received. Cannot be {@link Stage#SEND_INSTRUCTIONS}
     * @param trackingData the contact's {@link TrackingData}
     * @param rawContent the <b>unencrypted</b> raw content of the action. This <b>doesn't contain the payload type and action name</b>.
     *                   When the stage's payload type is an {@linkplain PayloadType#INSTRUCTION instruction} one, it is always empty. (aka {@code ""})
     */
    public abstract void onReceive(
            @NonNull Context context,
            @NonNull String sender,
            @NonNull BroadcastReceiver.PendingResult pendingResult,
            @NonNull Stage stage,
            @NonNull TrackingData trackingData,
            @NonNull String rawContent
    );

    public boolean canSendAnyPayload(Context context) {
        return this.dependentSettings
                .stream()
                .allMatch(setting -> setting.getValue(context));
    }

    @NonNull
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "name='" + this.name + '\'' +
                "isVoid='" + this.isVoid() + '\'' +
                '}';
    }

    public static String validateName(String name) {
        final String regexPattern = "[a-zA-Z0-9-_]*";
        if (!name.matches(regexPattern))
            throw new RuntimeException("Action's name '" + name + "' doesn't respect regex " + regexPattern);

        return name;
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
