package net.dean.jraw.models;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import org.jetbrains.annotations.Nullable;

@AutoValue
public abstract class PersistedAuthData {
    @Nullable
    public abstract OAuthData getCurrent();

    @Nullable
    public abstract String getRefreshToken();

    /**
     * Attempts to simplify the data contained in this reference.
     *
     * Returns null if this object is not significant ({@link #isSignificant()} returns false). If the OAuthData is
     * expired, this method returns a new object with a null OAuthData.
     *
     * If nothing can be simplified, this object is returned.
     */
    @Nullable
    public final PersistedAuthData simplify() {
        if (!isSignificant())
            return null;

        if (getCurrent() != null && getCurrent().isExpired())
            return PersistedAuthData.create(null, getRefreshToken());

        return this;
    }

    /**
     * This object is said to be significant if there is either some non-null, unexpired OAuthData or a non-null refresh
     * token.
     */
    public final boolean isSignificant() {
        return (getCurrent() != null && !getCurrent().isExpired()) || getRefreshToken() != null;
    }

    public static PersistedAuthData create(@Nullable OAuthData current, @Nullable String refreshToken) {
        return new AutoValue_PersistedAuthData(current, refreshToken);
    }

    public static JsonAdapter<PersistedAuthData> jsonAdapter(Moshi moshi) {
        return new AutoValue_PersistedAuthData.MoshiJsonAdapter(moshi);
    }
}
