package org.camunda.bpm.swagger.maven.model;

import static org.camunda.bpm.swagger.maven.GenerateSwaggerServicesMojo.CAMUNDA_REST_ROOT_PKG;

import java.lang.reflect.Field;

import javax.annotation.Generated;

import org.camunda.bpm.swagger.docs.model.RestService;
import org.camunda.bpm.swagger.maven.GenerateSwaggerServicesMojo;

import com.helger.jcodemodel.JCodeModel;
import com.helger.jcodemodel.JDefinedClass;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.SneakyThrows;

@EqualsAndHashCode(exclude = { "codeModel", "definedClass" }, callSuper = false)
public class CamundaRestService extends AbstractModel {

  public static final String PACKAGE = CAMUNDA_REST_ROOT_PKG + ".swagger";

  @Getter
  private final Class<?> serviceInterfaceClass;

  @Getter
  private final Class<?> serviceImplClass;

  @Getter
  private final JCodeModel codeModel;

  @Getter
  private final JDefinedClass definedClass;

  @Getter
  private final RestService restService;



  @SneakyThrows
  public CamundaRestService(final ModelRepository repository, final Class<?> serviceInterfaceClass, final Class<?> serviceImplClass) {

    super(repository);

    if (!serviceInterfaceClass.isAssignableFrom(serviceImplClass)) {
      throw new IllegalStateException(String.format("%s does not implement %s", serviceImplClass, serviceInterfaceClass));
    }

    this.serviceInterfaceClass = serviceInterfaceClass;
    this.serviceImplClass = serviceImplClass;

    this.codeModel = new JCodeModel();
    this.codeModel._package(PACKAGE);
    this.definedClass = this.codeModel._class(getFullQualifiedName());
    this.definedClass.annotate(Generated.class).param("value", GenerateSwaggerServicesMojo.class.getCanonicalName());

    // add doc reference
    restService = new RestService();
    getModelRepository().addService(this.serviceInterfaceClass.getName(), restService);
  }

  @Override
  public String getFullQualifiedName() {
    return String.format("%s.%sSwagger", PACKAGE, getSimpleName());
  }

  @Override
  public String getSimpleName() {
    return serviceInterfaceClass.getSimpleName();
  }

  @Override
  public Package getPackage() {
    return serviceInterfaceClass.getPackage();
  }

  @Override
  public Class<?> getBaseClass() {
    return serviceInterfaceClass;
  }

  @SneakyThrows
  public String getPath() {
    final Field field = serviceInterfaceClass.getDeclaredField("PATH");
    return field != null ? (String) field.get(null) : "";
  }

}
