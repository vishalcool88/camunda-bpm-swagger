package org.camunda.bpm.swagger.maven.generator;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.camunda.bpm.swagger.docs.model.RestOperation;
import org.camunda.bpm.swagger.maven.generator.step.TypeStep;
import org.camunda.bpm.swagger.maven.model.DocStyle;
import org.camunda.bpm.swagger.maven.model.ModelRepository;

import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JCodeModel;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Holds information about the return type of the method.
 *
 * @author Simon Zambrovski, Holisticon AG.
 *
 */
@Slf4j
public class ReturnTypeInfo {

  @Getter
  private final Method method;
  @Getter
  private Class<?> rawType;
  @Getter
  private AbstractJClass[] parameterTypes;
  private final List<TypeStep> dtos = new ArrayList<>();
  private final ModelRepository modelRepository;
  private final JCodeModel codeModel;

  /**
   * Constructs the return type info for given method.
   *
   * @param method
   *          method to determine the return type for.
   */
  public ReturnTypeInfo(final ModelRepository modelRepository, final JCodeModel codeModel, final Method method) {
    this.codeModel = codeModel;
    this.method = method;
    this.modelRepository = modelRepository;
    extractReturnType(this.method);
  }

  void extractReturnType(final Method method) {
    final Type returnType = method.getGenericReturnType();
    Class<?> result;

    if (returnType instanceof ParameterizedType) {
      final ParameterizedType type = (ParameterizedType) returnType;
      result = (Class<?>) type.getRawType();

      AbstractJClass[] parameterClasses = new AbstractJClass[type.getActualTypeArguments().length];
      for (int i = 0; i < type.getActualTypeArguments().length; i++) {
        final Type typeArg = type.getActualTypeArguments()[i];
        if (typeArg instanceof Class<?>) {
          // plain class X -> List<X>
          final Class<?> candidateParameterClass = (Class<?>) typeArg;
          if (TypeHelper.isDto(candidateParameterClass)) {
            final TypeStep dto = new TypeStep(modelRepository, candidateParameterClass, codeModel);
            // collect DTO
            dtos.add(dto);
            // an DTO is always a class
            parameterClasses[i] = (AbstractJClass) dto.getType();
          } else {
            // non DTO
            parameterClasses[i] = codeModel.ref(candidateParameterClass);
          }

        } else if (typeArg instanceof ParameterizedType) {
          // parametrized type Map<?,?> -> List<Map<X,Y>>
          log.debug(
              "The return type was a class parametrized by a parametrized class. The generator doesn't support it yet. Falling back to the basic type for {}",
              method);
          parameterClasses = new AbstractJClass[0];
          break;
        }
      }

      this.parameterTypes = parameterClasses;

    } else {
      result = method.getReturnType();
    }
    this.rawType = result;
  }

  /**
   * Assign rest operation documentation to found DTOs.
   *
   * @param restOperation
   *          rest operation conatning docs.
   */
  public void documentationFound(final RestOperation restOperation) {
    for (final TypeStep dto : dtos) {
      modelRepository.addDoc(dto.getFullQualifiedName(), restOperation, DocStyle.TYPE_PARAM);
    }
  }

  /**
   * Sometimes implementations methods uses the sub-type of the interface method return type as a return type. In this case, the return type should be taken
   * from the implementation method.
   *
   * @param implementationMethods
   *          array of all candidate methods.
   */
  public ReturnTypeInfo applyImplementationMethods(final Method[] implementationMethods) {
    final MethodMatcher matcher = new MethodMatcher(this.method);
    Arrays.stream(implementationMethods).filter(matcher).findFirst().ifPresent(matchedMethod -> {
      extractReturnType(matchedMethod);
      log.warn("The implementing type returned a subclass of defined return type. This is not an error and the generator replaced return type in '{}' to '{}'",
          this.method, this.rawType);
    });
    return this;
  }

  /**
   * Matches to all methods with the same name and same parameters as original but having a different return type.
   */
  static class MethodMatcher implements Predicate<Method> {
    private final Method original;

    public MethodMatcher(final Method original) {
      this.original = original;
    }

    @Override
    public boolean test(final Method candidate) {
      final boolean result = original.getName().equals(candidate.getName()) // methods must have equal names
          && original.getParameterCount() == candidate.getParameterCount() // and same number of parameters
          && !original.getReturnType().equals(candidate.getReturnType()); // and different return types
      if (!result) {
        return false;
      }
      // check every parameter type
      for (int i = 0; i < original.getParameterCount(); i++) {
        if (!original.getParameterTypes()[i].equals(candidate.getParameterTypes()[i])) {
          // if parameter type are not matching, the implementation candidate is refused.
          return false;
        }
      }
      return true;
    }
  }

  public boolean isParametrized() {
    return this.parameterTypes != null && this.parameterTypes.length > 0;
  }

}
