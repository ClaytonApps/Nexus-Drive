# Regras de ProGuard para o Nexus Drive

# Modelos serializados via Gson — o R8 não pode renomear nem remover
# campos acessados por reflexão, senão a (de)serialização JSON quebra.
-keep class com.nexusdrive.app.data.model.** { *; }
-keep class com.nexusdrive.app.data.source.remote.** { *; }

# Gson precisa de assinaturas genéricas e das anotações @SerializedName.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
