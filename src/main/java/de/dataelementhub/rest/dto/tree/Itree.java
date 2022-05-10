package de.dataelementhub.rest.dto.tree;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Itree.
 */
public class Itree {
  // Attributes for li and a currently missing

  private String icon;
  private State state;

  @JsonProperty("a")
  private AttributesList anchorAttributes;

  @JsonProperty("li")
  private AttributesList listItemAttributes;

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  public AttributesList getAnchorAttributes() {
    return anchorAttributes;
  }

  public void setAnchorAttributes(AttributesList anchorAttributes) {
    this.anchorAttributes = anchorAttributes;
  }

  public AttributesList getListItemAttributes() {
    return listItemAttributes;
  }

  public void setListItemAttributes(AttributesList listItemAttributes) {
    this.listItemAttributes = listItemAttributes;
  }
}
