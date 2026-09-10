package br.com.sussmartcare.facility;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "br.com.sussmartcare.facility")
class ArchitectureTest {

  @ArchTest
  static final ArchRule layers =
      layeredArchitecture()
          .consideringAllDependencies()
          .layer("Domain")
          .definedBy("..domain..")
          .layer("Application")
          .definedBy("..application..")
          .layer("Adapters")
          .definedBy("..adapters..")
          .layer("Infrastructure")
          .definedBy("..infrastructure..")
          .whereLayer("Domain")
          .mayOnlyBeAccessedByLayers(
              "Application",
              "Adapters",
              "Infrastructure")
          .whereLayer("Application")
          .mayOnlyBeAccessedByLayers(
              "Adapters",
              "Infrastructure")
          .whereLayer("Adapters")
          .mayNotBeAccessedByAnyLayer();
}
