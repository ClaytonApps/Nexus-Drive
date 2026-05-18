# Regras de ProGuard para o Nexus

# Modelos serializados via Gson — o R8 não pode renomear nem remover
# campos acessados por reflexão, senão a (de)serialização JSON quebra.
-keep class app.nexus.mobile.data.model.** { *; }
-keep class app.nexus.mobile.data.source.remote.** { *; }

# Gson precisa de assinaturas genéricas e das anotações @SerializedName.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
