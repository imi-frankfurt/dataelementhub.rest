package de.dataelementhub.rest.dto.tree;

import com.fasterxml.jackson.annotation.JsonProperty;

public class State {

  private Boolean checked;
  private Boolean collapsed;
  private Boolean draggable;

  @JsonProperty("drop-target")
  private Boolean dropTarget;

  private Boolean editable;
  private Boolean focused;
  private Boolean hidden;
  private Boolean indeterminate;
  private Boolean loading;
  private Boolean matched;
  private Boolean removed;
  private Boolean rendered;
  private Boolean selectable;
  private Boolean selected;

  public Boolean getChecked() {
    return checked;
  }

  public void setChecked(Boolean checked) {
    this.checked = checked;
  }

  public Boolean getCollapsed() {
    return collapsed;
  }

  public void setCollapsed(Boolean collapsed) {
    this.collapsed = collapsed;
  }

  public Boolean getDraggable() {
    return draggable;
  }

  public void setDraggable(Boolean draggable) {
    this.draggable = draggable;
  }

  public Boolean getDropTarget() {
    return dropTarget;
  }

  public void setDropTarget(Boolean dropTarget) {
    this.dropTarget = dropTarget;
  }

  public Boolean getEditable() {
    return editable;
  }

  public void setEditable(Boolean editable) {
    this.editable = editable;
  }

  public Boolean getFocused() {
    return focused;
  }

  public void setFocused(Boolean focused) {
    this.focused = focused;
  }

  public Boolean getHidden() {
    return hidden;
  }

  public void setHidden(Boolean hidden) {
    this.hidden = hidden;
  }

  public Boolean getIndeterminate() {
    return indeterminate;
  }

  public void setIndeterminate(Boolean indeterminate) {
    this.indeterminate = indeterminate;
  }

  public Boolean getLoading() {
    return loading;
  }

  public void setLoading(Boolean loading) {
    this.loading = loading;
  }

  public Boolean getMatched() {
    return matched;
  }

  public void setMatched(Boolean matched) {
    this.matched = matched;
  }

  public Boolean getRemoved() {
    return removed;
  }

  public void setRemoved(Boolean removed) {
    this.removed = removed;
  }

  public Boolean getRendered() {
    return rendered;
  }

  public void setRendered(Boolean rendered) {
    this.rendered = rendered;
  }

  public Boolean getSelectable() {
    return selectable;
  }

  public void setSelectable(Boolean selectable) {
    this.selectable = selectable;
  }

  public Boolean getSelected() {
    return selected;
  }

  public void setSelected(Boolean selected) {
    this.selected = selected;
  }
}
