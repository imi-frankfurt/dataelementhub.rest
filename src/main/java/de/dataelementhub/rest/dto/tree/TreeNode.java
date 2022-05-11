package de.dataelementhub.rest.dto.tree;

import java.util.Objects;

/**
 * Treenode.
 */
public class TreeNode {

  private String id;
  private String text;
  private Itree itree;
  private boolean children;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public Itree getItree() {
    return itree;
  }

  public void setItree(Itree itree) {
    this.itree = itree;
  }

  public boolean isChildren() {
    return children;
  }

  public void setChildren(boolean children) {
    this.children = children;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TreeNode treeNode = (TreeNode) o;
    return children == treeNode.children
        && Objects.equals(id, treeNode.id)
        && Objects.equals(text, treeNode.text)
        && Objects.equals(itree, treeNode.itree);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, text, itree, children);
  }
}
