package j2html.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.io.FileReader;
import java.nio.file.Path;

import static org.apache.commons.lang3.StringUtils.*;

public class TagCreatorGenerator {

  public void execute(Path tagClassesJsonPath, Path baseOutputDir) {

    TypeSpec.Builder tagCreatorSpec = TypeSpec.classBuilder(ClassName.get("j2html.tags", "TagCreator2"))
      .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

    try (FileReader fileReader = new FileReader(tagClassesJsonPath.toFile())) {
      JsonObject json = new Gson().fromJson(fileReader, JsonObject.class);

      json.getAsJsonArray("tags").forEach(element -> {
          String tag = element.isJsonObject() ? element.getAsJsonObject().get("tag").getAsString() : element.getAsString();
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
        ArrayTypeName domContentArrayTypeName = ArrayTypeName.of(ClassName.get("j2html.tags", "DomContent"));
        tagCreatorSpec.addMethod(
            MethodSpec.methodBuilder(tag)
              .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
              .returns(tagClassName)
              .addParameter(String.class, "text")
              .addParameter(domContentArrayTypeName, "dc")
              .varargs()
              .addStatement("return new $T().withText(text)", tagClassName)
              .build()
          );
        ClassName attrShortFormClassName = ClassName.get("j2html.attributes", "Attr").nestedClass("ShortForm");
        tagCreatorSpec.addMethod(
            MethodSpec.methodBuilder(tag)
              .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
              .returns(tagClassName)
              .addParameter(attrShortFormClassName, "shortAttr")
              .addStatement("return new $T().withAttr(shortAttr)", tagClassName)
              .build()
          );
        tagCreatorSpec.addMethod(
            MethodSpec.methodBuilder(tag)
              .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
              .returns(tagClassName)
              .addParameter(attrShortFormClassName, "shortAttr")
              .addParameter(String.class, "text")
              .addStatement("return new $T().withAttr(shortAttr).withText(text)", tagClassName)
              .build()
          );
        tagCreatorSpec.addMethod(
            MethodSpec.methodBuilder(tag)
              .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
              .returns(tagClassName)
              .addParameter(attrShortFormClassName, "shortAttr")
              .addParameter(domContentArrayTypeName, "dc")
              .addStatement("return new $T().withAttr(shortAttr).withText(text)", tagClassName)
              .build()
          );
        });

      JavaFile.builder("j2html.tags", tagCreatorSpec.build())
        .build()
        .writeTo(baseOutputDir);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
