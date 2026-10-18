package br.com.altapersonal.api;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import java.util.regex.Pattern;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Regras de dependência da arquitetura hexagonal, descritas em docs/ARCHITECTURE.md.
 * Se uma regra quebrar, corrija o código, não a regra.
 */
@AnalyzeClasses(packages = "br.com.altapersonal.api", importOptions = ArchitectureTest.ExcludeTestClasses.class)
class ArchitectureTest {

    /**
     * O DoNotIncludeTests do ArchUnit espera o build em {@code /build/}; no container ele fica em
     * {@code /gradle-build/}, então as classes de teste entrariam na análise.
     */
    static class ExcludeTestClasses implements ImportOption {

        private static final Pattern TEST_CLASSES = Pattern.compile(".*/classes/java/test/.*");

        @Override
        public boolean includes(Location location) {
            return !location.matches(TEST_CLASSES);
        }
    }

    @ArchTest
    static final ArchRule domainIsFrameworkFree = classes()
            .that().resideInAPackage("..domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "..domain..")
            .because("regra 1: o domínio é Java puro, sem Spring, JPA, Jackson ou adapters");

    @ArchTest
    static final ArchRule applicationOnlyUsesTransactionalFromSpring = classes()
            .that().resideInAPackage("..application..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                    "java..", "..domain..", "..application..", "org.springframework.transaction.annotation..")
            .because("regra 2: casos de uso só conhecem o domínio e as portas; do Spring, só @Transactional");

    @ArchTest
    static final ArchRule webAdapterTalksOnlyToInputPorts = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat(
                    resideInAnyPackage("..adapter.out..", "..application.service..", "..application.port.out..")
                            .or(resideInAPackage("..domain..")
                                    .and(not(resideInAPackage("br.com.altapersonal.api.shared..")))))
            .because("regra 3: adapters de entrada falam só com as portas de entrada, nunca com o domínio, "
                    + "services concretos ou persistência");

    @ArchTest
    static final ArchRule outputAdaptersDoNotDependOnInputAdapters = noClasses()
            .that().resideInAPackage("..adapter.out..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.in..")
            .because("regra 3: adapters de entrada e de saída não se conhecem");

    @ArchTest
    static final ArchRule persistenceStaysInPersistenceAdapter = noClasses()
            .that().resideOutsideOfPackage("..adapter.out.persistence..")
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.springframework.data..")
            .because("regra 4: JPA e Spring Data só existem dentro do adapter de persistência");

    @ArchTest
    static final ArchRule featuresDoNotDependOnEachOther = slices()
            .matching("br.com.altapersonal.api.(*)..")
            .should().notDependOnEachOther()
            .ignoreDependency(alwaysTrue(), resideInAPackage("br.com.altapersonal.api.shared.."))
            .ignoreDependency(alwaysTrue(), resideInAPackage("..application.port.in.."))
            .because("regra 5: uma feature só usa outra pelas portas de entrada dela");

    @ArchTest
    static final ArchRule onlyConfigKnowsServices = noClasses()
            .that().resideOutsideOfPackages("..config..", "..application.service..")
            .should().dependOnClassesThat().resideInAPackage("..application.service..")
            .because("regra 6: só o config da feature conhece os services concretos para montar os beans");
}
