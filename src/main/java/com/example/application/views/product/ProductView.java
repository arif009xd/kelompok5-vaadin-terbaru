package com.example.application.views.product;

import com.example.application.data.entity.Product;
import com.example.application.data.service.ProductService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@PageTitle("Product")
@Route(value = "product/:productID?/:action?(edit)", layout = MainLayout.class)
public class ProductView extends Div implements BeforeEnterObserver {

    private final String PRODUCT_ID = "productID";
    private final String PRODUCT_EDIT_ROUTE_TEMPLATE = "product/%s/edit";

    private final Grid<Product> grid = new Grid<>(Product.class, false);

    private TextField nameProduct;
    private TextField nameCategory;
    private TextField priceProduct;
    private TextField soldProduct;
    private TextField madeOn;
    private Upload thumbnailProduct;
    private Image thumbnailProductPreview;

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");

    private final BeanValidationBinder<Product> binder;

    private Product product;

    private final ProductService productService;

    public ProductView(ProductService productService) {
        this.productService = productService;
        addClassNames("product-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        grid.addColumn("nameProduct").setAutoWidth(true);
        grid.addColumn("nameCategory").setAutoWidth(true);
        grid.addColumn("priceProduct").setAutoWidth(true);
        grid.addColumn("soldProduct").setAutoWidth(true);
        grid.addColumn("madeOn").setAutoWidth(true);
        LitRenderer<Product> thumbnailProductRenderer = LitRenderer.<Product>of(
                "<span style='border-radius: 50%; overflow: hidden; display: flex; align-items: center; justify-content: center; width: 64px; height: 64px'><img style='max-width: 100%' src=${item.thumbnailProduct} /></span>")
                .withProperty("thumbnailProduct", item -> {
                    if (item != null && item.getThumbnailProduct() != null) {
                        return "data:image;base64," + Base64.getEncoder().encodeToString(item.getThumbnailProduct());
                    } else {
                        return "";
                    }
                });
        grid.addColumn(thumbnailProductRenderer).setHeader("Thumbnail Product").setWidth("96px").setFlexGrow(0);

        grid.setItems(query -> productService.list(
                PageRequest.of(query.getPage(), query.getPageSize(), VaadinSpringDataHelpers.toSpringDataSort(query)))
                .stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(PRODUCT_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(ProductView.class);
            }
        });

        // Configure Form
        binder = new BeanValidationBinder<>(Product.class);

        // Bind fields. This is where you'd define e.g. validation rules

        binder.bindInstanceFields(this);

        attachImageUpload(thumbnailProduct, thumbnailProductPreview);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.product == null) {
                    this.product = new Product();
                }
                binder.writeBean(this.product);
                productService.update(this.product);
                clearForm();
                refreshGrid();
                Notification.show("Data updated");
                UI.getCurrent().navigate(ProductView.class);
            } catch (ObjectOptimisticLockingFailureException exception) {
                Notification n = Notification.show(
                        "Error updating the data. Somebody else has updated the record while you were making changes.");
                n.setPosition(Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (ValidationException validationException) {
                Notification.show("Failed to update the data. Check again that all values are valid");
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> productId = event.getRouteParameters().get(PRODUCT_ID).map(Long::parseLong);
        if (productId.isPresent()) {
            Optional<Product> productFromBackend = productService.get(productId.get());
            if (productFromBackend.isPresent()) {
                populateForm(productFromBackend.get());
            } else {
                Notification.show(String.format("The requested product was not found, ID = %s", productId.get()), 3000,
                        Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(ProductView.class);
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        nameProduct = new TextField("Name Product");
        nameCategory = new TextField("Name Category");
        priceProduct = new TextField("Price Product");
        soldProduct = new TextField("Sold Product");
        madeOn = new TextField("Made On");
        Label thumbnailProductLabel = new Label("Thumbnail Product");
        thumbnailProductPreview = new Image();
        thumbnailProductPreview.setWidth("100%");
        thumbnailProduct = new Upload();
        thumbnailProduct.getStyle().set("box-sizing", "border-box");
        thumbnailProduct.getElement().appendChild(thumbnailProductPreview.getElement());
        formLayout.add(nameProduct, nameCategory, priceProduct, soldProduct, madeOn, thumbnailProductLabel,
                thumbnailProduct);

        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);
    }

    private void attachImageUpload(Upload upload, Image preview) {
        ByteArrayOutputStream uploadBuffer = new ByteArrayOutputStream();
        upload.setAcceptedFileTypes("image/*");
        upload.setReceiver((fileName, mimeType) -> {
            uploadBuffer.reset();
            return uploadBuffer;
        });
        upload.addSucceededListener(e -> {
            StreamResource resource = new StreamResource(e.getFileName(),
                    () -> new ByteArrayInputStream(uploadBuffer.toByteArray()));
            preview.setSrc(resource);
            preview.setVisible(true);
            if (this.product == null) {
                this.product = new Product();
            }
            this.product.setThumbnailProduct(uploadBuffer.toByteArray());
        });
        preview.setVisible(false);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(Product value) {
        this.product = value;
        binder.readBean(this.product);
        this.thumbnailProductPreview.setVisible(value != null);
        if (value == null || value.getThumbnailProduct() == null) {
            this.thumbnailProduct.clearFileList();
            this.thumbnailProductPreview.setSrc("");
        } else {
            this.thumbnailProductPreview
                    .setSrc("data:image;base64," + Base64.getEncoder().encodeToString(value.getThumbnailProduct()));
        }

    }
}
