package de.dataelementhub.rest.controller.v1.ConceptAssociationController;

import static de.dataelementhub.rest.controller.v1.ApiVersion.API_VERSION;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import de.dataelementhub.model.service.Terminologies.LoincService;
import de.dataelementhub.model.service.Terminologies.LoincService.LoincConcept;

import java.util.*;
@Transactional
@RestController
@RequestMapping(API_VERSION + "/loinc")
public class LoincController {

    private final LoincService loincService;

    @Autowired
    public LoincController(LoincService loincService) {
        this.loincService = loincService;
    }

    /**
     * Search LOINC concepts using the Regenstrief API by free text.
     * Example: GET /v1/loinc/regenstrief/search?query=diabetes
     */
    @GetMapping("/regenstrief/search")
    public List<LoincConcept> searchViaRegenstrief(@RequestParam String query) {
        return loincService.searchViaRegenstriefApi(query);
    }
    /**
     * Search for a specific LOINC code using the Regenstrief API.
     * Example: GET /v1/loinc/regenstrief/lookup?code=2345-7
     */
    @GetMapping("/regenstrief/lookup")
    public ResponseEntity<?> lookupLoincCodeViaRegenstrief(@RequestParam (name = "query") String code) {
        Optional<LoincConcept> concept = loincService.searchByCodeViaRegenstrief(code);
        return concept.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
//    /**
//     * Lookup a specific LOINC code.
//     * Example: GET /api/v1/loinc/lookup?code=1234-5
//     */
//    @GetMapping("/lookup")
//    public ResponseEntity<?> lookupLoincCode(@RequestParam String code) {
//        Optional<LoincConcept> concept = loincService.lookupCode(code);
//        return concept.map(ResponseEntity::ok)
//                .orElseGet(() -> ResponseEntity.notFound().build());
//    }
//
//    /**
//     * Search LOINC concepts using the FHIR API by free text.
//     * Example: GET /api/v1/loinc/search?text=glucose
//     */
////    @GetMapping("/search")
////    public List<LoincConcept> searchLoincByText(@RequestParam String text) {
////        return loincService.searchByText(text);
////    }

}
