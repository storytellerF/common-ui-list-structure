package com.storyteller_f.composite_compiler

import com.storyteller_f.composite_defination.Composite
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement

class CompositeAnnotation : AbstractProcessor() {
    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Composite::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(
        p0: MutableSet<out TypeElement>?,
        roundEnvironment: RoundEnvironment?
    ): Boolean {
        println("composite set:$p0 is over ${roundEnvironment?.processingOver()}")
        if (p0 == null || p0.isEmpty()) return false
        roundEnvironment?.getElementsAnnotatedWith(Composite::class.java)
            ?.forEach ElementRound@{ ele ->
                val packageElement = processingEnv.elementUtils.getPackageOf(ele)
                val databaseClass = ele.asType().toString()
                val dataDao = ele.enclosedElements?.firstOrNull { element ->
                    element.simpleName.toString().contains("Dao") && !element.simpleName.toString()
                        .contains("remoteKey")
                }?.simpleName ?: return@ElementRound
                val simpleName = ele.simpleName
                val annotation = ele.getAnnotation(Composite::class.java)
                val clazzList = ele.annotationMirrors.firstOrNull {
                    val toString = it.annotationType.toString()
                    toString.contains("Database")
                }?.elementValues?.filter {
                    it.key.toString().contains("entities")
                }?.firstNotNullOf { it.value }?.toString()?.let { s ->
                    s.substring(1, s.length - 1)
                }?.split(",")?.map { it.trim() }
                    ?.map { "import ${it.substring(0, it.length - 6)};" } ?: listOf()
                val name = if (annotation.name.trim().isNotEmpty()) annotation.name
                else getName(simpleName.toString())
                val packageName = packageElement.toString()
                val fileContent =
                    produceFileContent(name, packageName, dataDao, clazzList, databaseClass)
                processingEnv.filer.createSourceFile("${packageElement}.composite.${name}Composite", ele)
                    .openWriter()?.use {
                        it.write(fileContent)
                        it.flush()
                    }
            }

        return true
    }

    private fun getName(origin: String): String {
        for (i in 1 until origin.length) {
            if (origin[i].isUpperCase()) {
                return origin.substring(0, i)
            }
        }
        return origin
    }

    private fun produceFileContent(
        name: String,
        packageName: String,
        dataDao: Name,
        clazzList: List<String>,
        databaseClass: String
    ): String {
        val lower = name.lowercase()
        val dataParam = "${lower}s"
        val databaseType = "${name}Database"
        val remoteKeyType = "${name}RemoteKey"
        return "package $packageName.composite;\n" +
                "import androidx.annotation.NonNull;\n" +
                "import androidx.annotation.Nullable;\n\n" +
                clazzList.joinToString("\n") +
                "\n" +
                "import $databaseClass;\n" +
                "import com.storyteller_f.ui_list.database.CommonRoomDatabase;\n" +
                "\n" +
                "import java.util.List;\n" +
                "\n" +
                "import kotlin.Unit;\n" +
                "import kotlin.coroutines.Continuation;\n" +
                "public class ${name}Composite extends CommonRoomDatabase<$name, $remoteKeyType, $databaseType> {\n" +
                "    public ${name}Composite(@NonNull $databaseType database) {\n" +
                "        super(database);\n" +
                "    }\n" +
                "\n" +
                "    @Nullable\n" +
                "    @Override\n" +
                "    public Object clearOld(@NonNull Continuation<? super Unit> \$completion) {\n" +
                "        getDatabase().$dataDao().clearRepos(\$completion);\n" +
                "        getDatabase().remoteKeyDao().clearRemoteKeys(\$completion);\n" +
                "        return null;\n" +
                "    }\n" +
                "\n" +
                "    @Nullable\n" +
                "    @Override\n" +
                "    public Object insertRemoteKey(@NonNull List<? extends $remoteKeyType> remoteKeys, @NonNull Continuation<? super Unit> \$completion) {\n" +
                "        getDatabase().remoteKeyDao().insertAll((List<RepoRemoteKey>) remoteKeys, \$completion);\n" +
                "        return null;\n" +
                "    }\n" +
                "\n" +
                "    @Nullable\n" +
                "    @Override\n" +
                "    public Object getRemoteKey(@NonNull String id, @NonNull Continuation<? super RepoRemoteKey> \$completion) {\n" +
                "        return getDatabase().remoteKeyDao().remoteKeysRepoId(id, \$completion);\n" +
                "    }\n" +
                "\n" +
                "    @Nullable\n" +
                "    @Override\n" +
                "    public Object insertAllData(@NonNull List<? extends $name> ${dataParam}, @NonNull Continuation<? super Unit> \$completion) {\n" +
                "        getDatabase().$dataDao().insertAll((List<$name>) $dataParam, \$completion);\n" +
                "        return null;\n" +
                "    }\n" +
                "\n" +
                "    @Nullable\n" +
                "    @Override\n" +
                "    public Object deleteItemBy(@NonNull $name $lower, @NonNull Continuation<? super Unit> \$completion) {\n" +
                "        getDatabase().$dataDao().delete($lower, \$completion);\n" +
                "        getDatabase().remoteKeyDao().delete($lower.remoteKeyId(), \$completion);\n" +
                "        return null;\n" +
                "    }\n" +
                "\n" +
                "    @Nullable\n" +
                "    @Override\n" +
                "    public Object deleteItemById(@NonNull String commonDatumId, @NonNull Continuation<? super Unit> \$completion) {\n" +
                "        getDatabase().$dataDao().delete(Long.parseLong(commonDatumId), \$completion);\n" +
                "        getDatabase().remoteKeyDao().delete(commonDatumId, \$completion);\n" +
                "        return null;\n" +
                "    }\n" +
                "}"
    }

}