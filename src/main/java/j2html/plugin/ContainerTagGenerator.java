package j2html.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

import static org.apache.commons.lang3.StringUtils.*;

public class ContainerTagGenerator {

    public void execute(Path jsonFile, Path baseOutputDir) {

        TypeSpec.Builder tagCreatorSpec = TypeSpec.classBuilder(ClassName.get("j2html.tags", "TagCreator2"))
          .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

        try (FileReader fileReader = new FileReader(jsonFile.toFile())) {
            JsonObject json = new Gson().fromJson(fileReader, JsonObject.class);
            ClassName containerTagClassName = ClassName.get("j2html.tags", "ContainerTag");
            json.getAsJsonArray("tags").forEach(element -> {
                JsonObject tagJson = element.getAsJsonObject();

                String tag = tagJson.get("tag").getAsString();
                ClassName tagClassName = ClassName.get("j2html.tags", capitalize(tag + "Tag"));

                tagCreatorSpec.addMethod(
                  MethodSpec.methodBuilder(tag)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(tagClassName)
                    .addStatement("return new $T()", tagClassName)
                    .build()
                );
                tagCreatorSpec.addMethod(
                  MethodSpec.methodBuilder(tag)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(tagClassName)
                    .addParameter(String.class, "text")
                    .addStatement("return new $T().withText(text)", tagClassName)
                    .build()
                );
                tagCreatorSpec.addMethod(
                  MethodSpec.methodBuilder(tag)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(tagClassName)
                    .addParameter(String.class, "text")
                    .addParameter(ArrayTypeName.of(ClassName.get("j2html.tags", "DomContent")), "dc")
                    .varargs()
                    .addStatement("return new $T().withText(text)", tagClassName)
                    .build()
                );

                TypeSpec.Builder typeSpec = TypeSpec.classBuilder(tagClassName)
                  .superclass(ParameterizedTypeName.get(containerTagClassName, tagClassName))
                  .addMethod(
                    MethodSpec.constructorBuilder()
                      .addModifiers(Modifier.PUBLIC)
                      .addStatement("super(\"" + tag + "\")")
                      .build());

                JsonArray attrs = tagJson.getAsJsonArray("attrs");
                attrs.forEach(attrElement -> {
                    String attrName = attrElement.isJsonObject() ? attrElement.getAsJsonObject().get("name").getAsString() : attrElement.getAsString();
                    String attrNameCapitalized = capitalize(attrName);

                    MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("with" + attrNameCapitalized)
                      .addModifiers(Modifier.PUBLIC)
                      .returns(tagClassName);

                    MethodSpec.Builder condMethodSpec = MethodSpec.methodBuilder("withCond" + attrNameCapitalized)
                      .addModifiers(Modifier.PUBLIC)
                      .addParameter(boolean.class, "condition")
                      .returns(tagClassName);

                    if (attrElement.isJsonObject() && new JsonPrimitive(true).equals(attrElement.getAsJsonObject().get("empty"))) {
                        methodSpec.addStatement("return attr(\"" + attrName + "\")");
                        condMethodSpec.addStatement("return condAttr(condition, \"" + attrName + "\", \"" + attrName + "\")");
                    } else {
                        methodSpec
                          .addParameter(String.class, attrName)
                          .addStatement("return attr(\"" + attrName + "\", " + attrName + ")");
                        condMethodSpec
                          .addParameter(String.class, attrName)
                          .addStatement("return condAttr(condition, \"" + attrName + "\", " + attrName + ")");
                    }
                    typeSpec.addMethod(methodSpec.build())
                      .addMethod(condMethodSpec.build());

                    try {
                        JavaFile.builder("j2html.tags", typeSpec.build())
                          .build()
                          .writeTo(baseOutputDir);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            JavaFile.builder("j2html.tags", tagCreatorSpec.build())
              .build()
              .writeTo(baseOutputDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
