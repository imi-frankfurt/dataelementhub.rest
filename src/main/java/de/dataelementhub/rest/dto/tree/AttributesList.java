package de.dataelementhub.rest.dto.tree;

import java.util.HashMap;
import java.util.Map;

/**
 * Attributes List.
 */
public class AttributesList {

  private Map<String, String> attributes;

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  /**
   * TODO: add javadoc.
   */
  public void addAttribute(String key, String value) {
    if (attributes == null) {
      attributes = new HashMap<>();
    }
    attributes.put(key, value);
  }
}
