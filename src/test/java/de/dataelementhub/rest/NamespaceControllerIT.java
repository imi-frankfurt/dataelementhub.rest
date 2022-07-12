package de.dataelementhub.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.dataelementhub.model.dto.element.Namespace;
import de.dataelementhub.rest.controller.v1.NamespaceController;
import java.net.URI;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@Tag("namespace")
@ExtendWith(SpringExtension.class)
@WebMvcTest(
    controllers = NamespaceController.class
)
public class NamespaceControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper jsonUtil;

  @Test
  public void testCreateNamespace_FailsOnInvalidNamespace() throws Exception {
    Namespace namespace = new Namespace();

    mockMvc.perform(post(URI.create("/v1/namespaces"))
            .contentType(APPLICATION_JSON)
            .content(jsonUtil.writeValueAsString(namespace)))
        .andExpect(status().isBadRequest());
  }
}
