package de.dataelementhub.rest.controller.v1;

import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.rest.dto.tree.AttributesList;
import de.dataelementhub.rest.dto.tree.Itree;
import de.dataelementhub.rest.dto.tree.State;
import de.dataelementhub.rest.dto.tree.TreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jooq.CloseableDSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tree Controller.
 */
@RestController
@RequestMapping("/v1/tree")
public class TreeController {

  private static final Logger LOGGER = LoggerFactory.getLogger(TreeController.class);

  public static final String NODE_PREFIX_NAMESPACE = "ns_";
  public static final String ATTRIBUTE_ELEMENTTYPE = "data-elementtype";

  /**
   * TODO.
   */
  @GetMapping("/{nodeId}")
  public List<TreeNode> get(@PathVariable String nodeId, @RequestParam List<Status> status) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      /*DehubUser user = DataElementHubRestApplication.getCurrentUser();
      if (nodeId.equals("root")) {
        List<DescribedElement> namespaces = NamespaceDao.getReadableNamespaces(ctx, user.getId());
        return createInitialTree(
            namespaces.stream()
                .map(e -> ((Namespace) e.getElement()).getName())
                .collect(Collectors.toList()));
      } else {
        List<IdentifiedElement> elements;
        if (nodeId.startsWith(NODE_PREFIX_NAMESPACE)) {
          String namespace = nodeId.substring(NODE_PREFIX_NAMESPACE.length());
          elements = IdentifiedDao.getRootElements(ctx, user.getId(), namespace, status);
        } else {
          elements = IdentifiedDao.getChildren(ctx, user.getId(), nodeId, status);
        }
        return createTreeNodes(elements);
      }*/
      return new ArrayList<>(); // todo remove
    }
  }

  /** TODO: add javadoc. */
  public static List<TreeNode> createInitialTree(List<String> namespaceNames) {
    List<TreeNode> rootNodeList = new ArrayList<>();

    for (String namespace : namespaceNames) {
      createTreeNode(rootNodeList, namespace, NODE_PREFIX_NAMESPACE + namespace, true);
    }
    // Make namespaces non-selectable
    for (TreeNode rootNode : rootNodeList) {
      Itree rootNodeItree = rootNode.getItree();
      if (rootNodeItree == null) {
        rootNodeItree = new Itree();
      }
      State rootNodeItreeState = rootNodeItree.getState();
      if (rootNodeItreeState == null) {
        rootNodeItreeState = new State();
      }
      rootNodeItreeState.setSelectable(false);
      rootNodeItree.setState(rootNodeItreeState);
      rootNode.setItree(rootNodeItree);
    }

    return rootNodeList;
  }

  /** TODO: add javadoc. */
  /*public static List<TreeNode> createTreeNodes(List<IdentifiedElement> elements) {
    List<TreeNode> nodeList = new ArrayList<>();

    for (IdentifiedElement element : elements) {
      Map<String, String> attributeMap = new HashMap<>();
      // TODO: we should have a preferred language, a fallback (e.g. EN)
      //  and then the first element.
      String designation = element.getDefinitions().get(0).getDesignation();
      String urn = element.getScoped().toString();

      switch (element.getElementType()) {
        case DATAELEMENT:
          attributeMap.put(ATTRIBUTE_ELEMENTTYPE, "dataelement");
          createTreeNode(
              nodeList,
              designation,
              urn,
              false,
              attributeMap);
          break;
        case DATAELEMENTGROUP:
          attributeMap.put(ATTRIBUTE_ELEMENTTYPE, "dataelementgroup");
          createTreeNode(
              nodeList,
              designation,
              urn,
              true,
              attributeMap);
          break;
        case RECORD:
          attributeMap.put(ATTRIBUTE_ELEMENTTYPE, "record");
          createTreeNode(
              nodeList,
              designation,
              urn,
              true,
              attributeMap);
          break;
        default:
          break;
      }
    }

    return nodeList;
  }*/

  private static void createTreeNode(
      List<TreeNode> rootNodeList, String name, String id, boolean children) {
    createTreeNode(rootNodeList, name, id, children, null, null);
  }

  private static void createTreeNode(
      List<TreeNode> rootNodeList,
      String name,
      String id,
      boolean children,
      Map<String, String> anchorAttributes) {
    createTreeNode(rootNodeList, name, id, children, anchorAttributes, null);
  }

  private static void createTreeNode(
      List<TreeNode> rootNodeList,
      String name,
      String id,
      boolean children,
      Map<String, String> anchorAttributes,
      Map<String, String> listItemAttributes) {
    TreeNode groupNode = new TreeNode();
    groupNode.setText(name);
    groupNode.setId(id);
    groupNode.setChildren(children);

    if (anchorAttributes != null || listItemAttributes != null) {
      Itree itree = new Itree();

      if (anchorAttributes != null) {
        AttributesList list = new AttributesList();
        for (Map.Entry<String, String> anchorAttribute : anchorAttributes.entrySet()) {
          list.addAttribute(anchorAttribute.getKey(), anchorAttribute.getValue());
        }
        itree.setAnchorAttributes(list);
      }

      if (listItemAttributes != null) {
        AttributesList liList = new AttributesList();
        for (Map.Entry<String, String> liAttribute : listItemAttributes.entrySet()) {
          liList.addAttribute(liAttribute.getKey(), liAttribute.getValue());
        }
        itree.setListItemAttributes(liList);
      }

      // Make everything but imports unselectable
      State state = new State();
      if (anchorAttributes != null
          && !"import".equalsIgnoreCase(anchorAttributes.get(ATTRIBUTE_ELEMENTTYPE))) {
        state.setSelectable(false);
      }
      itree.setState(state);

      groupNode.setItree(itree);
    }

    rootNodeList.add(groupNode);
  }
}
