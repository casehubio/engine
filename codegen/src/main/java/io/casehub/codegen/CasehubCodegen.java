package io.casehub.codegen;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sun.codemodel.JCodeModel;

import java.io.File;
import java.io.IOException;

import org.jsonschema2pojo.ContentResolver;
import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.RuleLogger;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.SourceType;
import org.jsonschema2pojo.rules.RuleFactory;

public class CasehubCodegen {

  public static void main(String[] args) throws IOException {
    if (args.length < 3) {
      System.err.println("Usage: CasehubCodegen <sourceFile> <outputDir> <targetPackage>");
      System.exit(1);
    }

    File sourceFile = new File(args[0]);
    File outputDir = new File(args[1]);
    String targetPackage = args[2];

    GenerationConfig config = new DefaultGenerationConfig() {
      @Override
      public SourceType getSourceType() {
        return SourceType.YAMLSCHEMA;
      }

      @Override
      public boolean isUsePrimitives() {
        return false;
      }

      @Override
      public boolean isIncludeAdditionalProperties() {
        return false;
      }
    };

    ContentResolver contentResolver = new ContentResolver(new YAMLFactory());
    SchemaStore schemaStore = new SchemaStore(contentResolver, new RuleLogger() {
      public void debug(String msg) {
      }

      public void error(String msg) {
        System.err.println(msg);
      }

      public void error(String msg, Throwable t) {
        System.err.println(msg);
        t.printStackTrace();
      }

      public void info(String msg) {
      }

      public void trace(String msg) {
      }

      public void warn(String msg) {
        System.err.println("WARN: " + msg);
      }

      public void warn(String msg, Throwable t) {
        System.err.println("WARN: " + msg);
      }

      public boolean isDebugEnabled() {
        return false;
      }

      public boolean isErrorEnabled() {
        return true;
      }

      public boolean isInfoEnabled() {
        return false;
      }

      public boolean isTraceEnabled() {
        return false;
      }

      public boolean isWarnEnabled() {
        return true;
      }
    });

    RuleFactory ruleFactory = new CasehubRuleFactory();
    ruleFactory.setGenerationConfig(config);
    ruleFactory.setAnnotator(new Jackson2Annotator(config));
    ruleFactory.setSchemaStore(schemaStore);

    SchemaMapper mapper = new SchemaMapper(ruleFactory, new SchemaGenerator());

    JCodeModel codeModel = new JCodeModel();
    mapper.generate(codeModel, "CaseHub", targetPackage, sourceFile.toURI().toURL());

    outputDir.mkdirs();
    codeModel.build(outputDir);
  }
}
