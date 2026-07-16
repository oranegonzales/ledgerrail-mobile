# Retrofit and Moshi publish consumer rules. Keep only the DTO constructors used
# by Moshi's Kotlin reflection adapter.
-keep class dev.oranegonzales.ledgerrail.mobile.data.api.** { *; }
