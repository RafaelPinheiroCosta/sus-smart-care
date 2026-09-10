package br.com.sussmartcare.facility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class FacilityOpenApiContractTest {

  @Test
  void contractDeclaresAllImplementedOperations()
      throws IOException {

    Path contract = locateContract();

    Map<String, Object> document;

    try (var reader = Files.newBufferedReader(contract)) {
      document =
          new Yaml().load(reader);
    }

    assertEquals(
        "3.0.3",
        document.get("openapi"));

    Map<String, Object> info =
        map(document.get("info"));

    assertEquals(
        "0.5.0",
        info.get("version"));

    Map<String, Object> paths =
        map(document.get("paths"));

    Map<String, String[]> expected =
        new LinkedHashMap<>();

    expected.put(
        "/api/v1/facilities",
        new String[]{"get", "post"});

    expected.put(
        "/api/v1/facilities/{facilityId}",
        new String[]{"get"});

    expected.put(
        "/api/v1/facilities/{facilityId}/status",
        new String[]{"patch"});

    expected.put(
        "/api/v1/facilities/{facilityId}/zones",
        new String[]{"get", "post"});

    expected.put(
        "/api/v1/zones/{zoneId}",
        new String[]{"get"});

    expected.put(
        "/api/v1/zones/{zoneId}/status",
        new String[]{"patch"});

    expected.put(
        "/api/v1/zones/{zoneId}/beds",
        new String[]{"get", "post"});

    expected.put(
        "/api/v1/beds/{bedId}",
        new String[]{"get"});

    expected.put(
        "/api/v1/beds/{bedId}/operational-status",
        new String[]{"patch"});

    expected.put(
        "/api/v1/beds/{bedId}/occupations",
        new String[]{"post"});

    expected.put(
        "/api/v1/beds/{bedId}/occupation",
        new String[]{"get"});

    expected.put(
        "/api/v1/bed-occupations/{occupationId}/release",
        new String[]{"post"});

    int operationCount = 0;

    for (Map.Entry<String, String[]> entry :
        expected.entrySet()) {

      assertTrue(
          paths.containsKey(entry.getKey()),
          () -> "Missing path: " + entry.getKey());

      Map<String, Object> path =
          map(paths.get(entry.getKey()));

      for (String method : entry.getValue()) {

        assertTrue(
            path.containsKey(method),
            () ->
                "Missing operation: " +
                    method.toUpperCase() +
                    " " +
                    entry.getKey());

        Map<String, Object> operation =
            map(path.get(method));

        assertNotNull(
            operation.get("operationId"));

        operationCount++;
      }
    }

    assertEquals(15, operationCount);
  }

  @Test
  void contractDeclaresBearerSecurityAndDomainSchemas()
      throws IOException {

    Map<String, Object> document;

    try (var reader =
        Files.newBufferedReader(locateContract())) {

      document =
          new Yaml().load(reader);
    }

    Map<String, Object> components =
        map(document.get("components"));

    Map<String, Object> securitySchemes =
        map(components.get("securitySchemes"));

    assertTrue(
        securitySchemes.containsKey("bearerAuth"));

    Map<String, Object> schemas =
        map(components.get("schemas"));

    assertTrue(
        schemas.containsKey("FacilityType"));

    assertTrue(
        schemas.containsKey("CareZoneType"));

    assertTrue(
        schemas.containsKey("BedType"));

    assertTrue(
        schemas.containsKey("BedOperationalStatus"));

    assertTrue(
        schemas.containsKey("UpdateActivationRequest"));

    assertTrue(
        schemas.containsKey(
            "UpdateBedOperationalStatusRequest"));

    assertTrue(
        schemas.containsKey(
            "BedOccupationResponse"));
  }

  private Path locateContract() {

    Path current =
        Path.of(
            System.getProperty("user.dir"))
            .toAbsolutePath();

    while (current != null) {

      Path candidate =
          current.resolve(
              "contracts/openapi/facility.yaml");

      if (Files.exists(candidate)) {
        return candidate;
      }

      current = current.getParent();
    }

    throw new IllegalStateException(
        "contracts/openapi/facility.yaml not found");
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> map(Object value) {

    assertNotNull(value);

    return (Map<String, Object>) value;
  }
}
