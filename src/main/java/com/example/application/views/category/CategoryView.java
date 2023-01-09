package com.example.application.views.category;

import com.example.application.data.entity.Category;
import com.example.application.data.service.CategoryService;
import com.example.application.views.MainLayout;
import com.example.application.views.product.ProductView;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.littemplate.LitTemplate;
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
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@PageTitle("Category")
@Route(value = "category/:categoryID?/:action?(edit)", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@Tag("category-view")
@JsModule("./views/category/category-view.ts")
public class CategoryView extends Div implements BeforeEnterObserver {

    private final String CATEGORY_ID = "categoryID";
    private final String CATEGORY_EDIT_ROUTE_TEMPLATE = "category/%s/edit";

    // This is the Java companion file of a design
    // You can find the design file inside /frontend/views/
    // The design can be easily edited by using Vaadin Designer
    // (vaadin.com/designer)

    private final Grid<Category> grid = new Grid<>(Category.class, false);


    private TextField nameCategory;

    private TextField slugProduct;

    private TextField totalProduct;
    private Upload thumbnailSlug;
    private Image thumbnailSlugPreview;

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");
    private final Button delete = new Button("Delete");

    private final BeanValidationBinder<Category> binder;

    private Category category;

    private final CategoryService categoryService;

    public CategoryView(CategoryService categoryService) {
        this.categoryService = categoryService;
        addClassNames("category-view");

        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        grid.addColumn("nameCategory").setAutoWidth(true);
        grid.addColumn("slugProduct").setAutoWidth(true);
        grid.addColumn("totalProduct").setAutoWidth(true);
        LitRenderer<Category> thumbnailSlugRenderer = LitRenderer.<Category>of(
                "<span style='border-radius: 50%; overflow: hidden; display: flex; align-items: center; justify-content: center; width: 64px; height: 64px'><img style='max-width: 100%' src=${item.thumbnailSlug} /></span>")
                .withProperty("thumbnailSlug", item -> {
                    if (item != null && item.getThumbnailSlug() != null) {
                        return "data:image;base64," + Base64.getEncoder().encodeToString(item.getThumbnailSlug());
                    } else {
                        return "";
                    }
                });
        grid.addColumn(thumbnailSlugRenderer).setHeader("Thumbnail Slug").setWidth("96px").setFlexGrow(0);

        grid.setItems(query -> categoryService.list(
                PageRequest.of(query.getPage(), query.getPageSize(), VaadinSpringDataHelpers.toSpringDataSort(query)))
                .stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.setHeightFull();

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(CATEGORY_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(CategoryView.class);
            }
        });

        // Configure Form
        binder = new BeanValidationBinder<>(Category.class);

        // Bind fields. This is where you'd define e.g. validation rules

        binder.bindInstanceFields(this);

        attachImageUpload(thumbnailSlug, thumbnailSlugPreview);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.category == null) {
                    this.category = new Category();
                }
                binder.writeBean(this.category);
                categoryService.update(this.category);
                clearForm();
                refreshGrid();
                Notification.show("Data updated");
                UI.getCurrent().navigate(CategoryView.class);
            } catch (ObjectOptimisticLockingFailureException exception) {
                Notification n = Notification.show(
                        "Error updating the data. Somebody else has updated the record while you were making changes.");
                n.setPosition(Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (ValidationException validationException) {
                Notification.show("Failed to update the data. Check again that all values are valid");
            }
        });

        delete.addClickListener(e -> {
            try {
                if (this.category == null) {
                    Notification.show("no category selection");
                }else {
                    binder.writeBean(this.category);
                    categoryService.delete(this.category.getId());
                    clearForm();
                    refreshGrid();
                    Notification.show("Data deleted");
                    UI.getCurrent().navigate(ProductView.class);
                }
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
        Optional<Long> categoryId = event.getRouteParameters().get(CATEGORY_ID).map(Long::parseLong);
        if (categoryId.isPresent()) {
            Optional<Category> categoryFromBackend = categoryService.get(categoryId.get());
            if (categoryFromBackend.isPresent()) {
                populateForm(categoryFromBackend.get());
            } else {
                Notification.show(String.format("The requested category was not found, ID = %s", categoryId.get()),
                        3000, Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(CategoryView.class);
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
        nameCategory = new TextField("Name Category");
        slugProduct = new TextField("Slug");
        totalProduct = new TextField("Total Product");
        Label thumbnailSlugLabel = new Label("Thumbnail Product");
        thumbnailSlugPreview = new Image();
        thumbnailSlugPreview.setWidth("100%");
        thumbnailSlug = new Upload();
        thumbnailSlug.getStyle().set("box-sizing", "border-box");
        thumbnailSlug.getElement().appendChild(thumbnailSlugPreview.getElement());
        formLayout.add( nameCategory, slugProduct, totalProduct, thumbnailSlugLabel,
                thumbnailSlug);

        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, delete, cancel);
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
            if (this.category == null) {
                this.category = new Category();
            }
            this.category.setThumbnailSlug(uploadBuffer.toByteArray());
        });
        preview.setVisible(false);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getLazyDataView().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(Category value) {
        this.category = value;
        binder.readBean(this.category);
        this.thumbnailSlugPreview.setVisible(value != null);
        if (value == null || value.getThumbnailSlug() == null) {
            this.thumbnailSlug.clearFileList();
            this.thumbnailSlugPreview.setSrc("");
        } else {
            this.thumbnailSlugPreview
                    .setSrc("data:image;base64," + Base64.getEncoder().encodeToString(value.getThumbnailSlug()));
        }

    }
}
