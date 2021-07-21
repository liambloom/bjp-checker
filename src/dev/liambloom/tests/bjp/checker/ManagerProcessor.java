package dev.liambloom.tests.bjp.checker;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Set;

@SupportedAnnotationTypes("dev.liambloom.tests.bjp.checker.Managed")
// @SupportedSourceVersion(< source version >)
public class ManagerProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element managedElement : annotatedElements) {
                TypeElement managed = ((TypeElement) managedElement);
                if (managed.getKind() == ElementKind.INTERFACE || managed.getModifiers().contains(Modifier.ABSTRACT)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@Managed must be applied to an interface or abstract class", managed /*, TODO */);
                    continue;
                }

                String className = managed.getQualifiedName() + "Manager";
                JavaFileObject manager;
                PrintWriter out;
                try {
                    manager = processingEnv.getFiler().createSourceFile(className);
                    out = new PrintWriter(manager.openWriter());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }

                out.print("package ");
                out.print(processingEnv.getElementUtils().getPackageOf(managed).getQualifiedName());
                out.println(";");
                out.println();

                // add imports
                // Actually, it might just be better to use everything's fully qualified names

                out.print("public class ");
                out.print(className);
                out.println(" {");

                out.print("    private ");
                out.print(className);
                out.println("() {}");

                for (ExecutableElement method :  ElementFilter.methodsIn(managed.getEnclosedElements())) {
                    if (method.getModifiers().contains(Modifier.STATIC))
                        continue;

                    if (!method.getSimpleName().subSequence(0, 3).equals("get") || !method.getParameters().isEmpty() || method.getReturnType().getKind() != TypeKind.VOID) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                "Each non-static method in a @Managed type must be a getXxx methods with no arguments and a non-void return type", method);
                        continue;
                    }

                    // method.getReturnType()
                }
            }
        }

        return true;
    }


}
