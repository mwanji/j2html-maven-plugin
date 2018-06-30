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

  public void execute(Path jsonFile, Path globalAttributesJsonPath, Path baseOutputDir) {
    Gson gson = new Gson();

    try (FileReader fileReader = new FileReader(jsonFile.toFile()); FileReader globalAttributesFileReader = new FileReader(globalAttributesJsonPath.toFile())) {
      JsonObject json = gson.fromJson(fileReader, JsonObject.class);
      JsonArray globalAttributesJson = gson.fromJson(globalAttributesFileReader, JsonArray.class);

      ClassName containerTagClassName = ClassName.get("j2html.tags", "ContainerTag");
      json.getAsJsonArray("tags").forEach(element -> {
        String tag = element.isJsonObject() ? element.getAsJsonObject().get("tag").getAsString() : element.getAsString();
        ClassName tagClassName = ClassName.get("j2html.tags", capitalize(tag + "Tag"));

        TypeSpec.Builder typeSpec = TypeSpec.classBuilder(tagClassName)
          .superclass(ParameterizedTypeName.get(containerTagClassName, tagClassName))
          .addMethod(
            MethodSpec.constructorBuilder()
              .addModifiers(Modifier.PUBLIC)
              .addStatement("super(\"" + tag + "\")")
              .build());

        ClassName attrClassName = ClassName.get("j2html.attributes", "Attr");
        typeSpec.addMethod(MethodSpec.methodBuilder("withAttr")
          .addModifiers(Modifier.PUBLIC)
          .addParameter(attrClassName.nestedClass("ShortForm"), "attr")
          .returns(tagClassName)
          .addStatement("$T.addTo(this, attr)", attrClassName)
          .addStatement("return this")
          .build());

        if (element.isJsonObject()) {
          JsonObject tagJson = element.getAsJsonObject();
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
          });
        }


        globalAttributesJson.forEach(globalAttributeJsonElement -> {
          String name = globalAttributeJsonElement.isJsonObject() ? globalAttributeJsonElement.getAsJsonObject().get("name").getAsString() : globalAttributeJsonElement.getAsString();
          ClassName attributeClassName = globalAttributeJsonElement.isJsonObject() && globalAttributeJsonElement.getAsJsonObject().has("type") ? ClassName.get("java.lang", globalAttributeJsonElement.getAsJsonObject().get("type").getAsString()) : ClassName.get(String.class);

          typeSpec.addMethod(MethodSpec.methodBuilder("with" + capitalize(name))
              .addParameter(attributeClassName, name)
              .addStatement("return attr(\"" + name + "\", " + name + ")")
              .returns(tagClassName)
              .build())
            .addMethod(MethodSpec.methodBuilder("withCond" + capitalize(name))
              .addParameter(boolean.class, "condition")
              .addParameter(attributeClassName, name)
              .addStatement("return condAttr(condition, \"" + name + "\", " + name + ")")
              .returns(tagClassName)
              .build());
        });

        try {
          JavaFile.builder("j2html.tags", typeSpec.build())
            .build()
            .writeTo(baseOutputDir);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
