package fi.dy.masa.malilib.gui.config.indicator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import fi.dy.masa.malilib.gui.BaseListScreen;
import fi.dy.masa.malilib.gui.config.ConfigTab;
import fi.dy.masa.malilib.gui.config.ConfigTabRegistryImpl;
import fi.dy.masa.malilib.gui.widget.DropDownListWidget;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.list.DataListWidget;
import fi.dy.masa.malilib.gui.widget.list.entry.ConfigInfoEntryWidget;
import fi.dy.masa.malilib.overlay.widget.ConfigStatusIndicatorContainerWidget;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ConfigOnTab;
import fi.dy.masa.malilib.util.data.ModInfo;
import fi.dy.masa.malilib.util.data.NameIdentifiable;

public class ConfigStatusIndicatorGroupAddConfigsScreen extends BaseListScreen<DataListWidget<ConfigOnTab>>
{
    protected final ConfigStatusIndicatorContainerWidget widget;
    protected final DropDownListWidget<ModInfo> modsDropDownWidget;
    protected DropDownListWidget<ConfigTab> categoriesDropDownWidget;
    protected final GenericButton addEntriesButton;
    protected final List<ConfigTab> currentCategories = new ArrayList<>();

    protected ConfigStatusIndicatorGroupAddConfigsScreen(ConfigStatusIndicatorContainerWidget widget)
    {
        super(10, 68, 20, 70);

        this.widget = widget;
        this.useTitleHierarchy = false;
        this.setTitle("malilib.gui.title.config_status_indicator_configuration");

        List<ModInfo> mods = new ArrayList<>();
        mods.add(null);
        mods.addAll(((ConfigTabRegistryImpl) Registry.CONFIG_TAB).getAllModsWithConfigTabs());

        this.modsDropDownWidget = new DropDownListWidget<>(-1, 14, 160, 10, mods, ModInfo::getModName);
        this.modsDropDownWidget.setSelectionListener(this::onModSelected);

        this.categoriesDropDownWidget = new DropDownListWidget<>(-1, 14, 160, 10, this.currentCategories, ConfigTab::getDisplayName);
        this.categoriesDropDownWidget.setSelectionListener(this::onCategorySelected);

        this.addEntriesButton = GenericButton.simple("malilib.gui.button.add_selected_configs", this::addSelectedConfigs);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.modsDropDownWidget);
        this.addWidget(this.addEntriesButton);

        if (this.modsDropDownWidget.getSelectedEntry() != null)
        {
            this.addWidget(this.categoriesDropDownWidget);
        }
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int x = this.x + 10;
        int y = this.y + 24;

        this.modsDropDownWidget.setPosition(x, y);
        this.categoriesDropDownWidget.setPosition(this.modsDropDownWidget.getRight() + 8, y);

        y += 20;
        this.addEntriesButton.setPosition(x, y);
    }

    @Nullable
    @Override
    protected DataListWidget<ConfigOnTab> createListWidget(int listX, int listY, int listWidth, int listHeight)
    {
        DataListWidget<ConfigOnTab> listWidget = new DataListWidget<>(listX, listY, listWidth, listHeight, this::getFilteredConfigs);
        listWidget.setEntryWidgetFactory(ConfigInfoEntryWidget::new);
        listWidget.setFetchFromSupplierOnRefresh(true);
        listWidget.getEntrySelectionHandler().setAllowSelection(true);
        listWidget.getEntrySelectionHandler().setAllowMultiSelection(true);
        listWidget.getBorderRenderer().getNormalSettings().setBorderWidth(1);
        listWidget.setListEntryWidgetFixedHeight(15);

        return listWidget;
    }

    protected void onModSelected(@Nullable ModInfo mod)
    {
        this.currentCategories.clear();

        Supplier<List<ConfigTab>> tabProvider = mod != null ? Registry.CONFIG_TAB.getConfigTabProviderFor(mod) : null;

        if (tabProvider != null)
        {
            this.currentCategories.addAll(tabProvider.get());
            this.currentCategories.sort(Comparator.comparing(ConfigTab::getDisplayName));
        }

        // TODO remove after the dropdown widget gets refresh support
        this.removeWidget(this.categoriesDropDownWidget);
        this.categoriesDropDownWidget = new DropDownListWidget<>(-1, 14, 160, 10, this.currentCategories, ConfigTab::getDisplayName);
        this.categoriesDropDownWidget.setSelectionListener(this::onCategorySelected);

        int x = this.modsDropDownWidget.getRight() + 8;
        int y = this.modsDropDownWidget.getY();
        this.categoriesDropDownWidget.setPosition(x, y);

        if (this.modsDropDownWidget.getSelectedEntry() != null)
        {
            this.addWidget(this.categoriesDropDownWidget);
        }
        else
        {
            this.removeWidget(this.categoriesDropDownWidget);
        }

        this.refreshList();
    }

    protected void onCategorySelected(@Nullable NameIdentifiable category)
    {
        this.refreshList();
    }

    protected void addSelectedConfigs()
    {
        ArrayList<ConfigOnTab> list = new ArrayList<>(this.getListWidget().getSelectedEntries());
        list.sort(Comparator.comparing(cot -> cot.config.getDisplayName()));

        for (ConfigOnTab config : list)
        {
            this.widget.addWidgetForConfig(config);
        }

        this.refreshList();
    }

    protected void refreshList()
    {
        this.getListWidget().getEntrySelectionHandler().clearSelection();
        this.getListWidget().refreshEntries();
    }

    protected List<ConfigOnTab> getFilteredConfigs()
    {
        final List<ConfigOnTab> configsInScope = new ArrayList<>();

        ModInfo mod = this.modsDropDownWidget.getSelectedEntry();

        if (mod == null)
        {
            return configsInScope;
        }

        Supplier<List<ConfigTab>> tabProvider = Registry.CONFIG_TAB.getConfigTabProviderFor(mod);

        if (tabProvider != null)
        {
            ConfigTab configTab = this.categoriesDropDownWidget.getSelectedEntry();

            if (configTab == null)
            {
                tabProvider.get().forEach((tab) -> this.addConfigsHavingStatusWidgetFactory(tab, configsInScope::add));
            }
            else
            {
                for (ConfigTab tab : tabProvider.get())
                {
                    if (tab == configTab)
                    {
                        this.addConfigsHavingStatusWidgetFactory(tab, configsInScope::add);
                        break;
                    }
                }
            }
        }

        HashSet<ConfigOnTab> existingConfigs = new HashSet<>(this.widget.getConfigs());
        configsInScope.removeAll(existingConfigs);

        return configsInScope;
    }

    protected void addConfigsHavingStatusWidgetFactory(ConfigTab tab, Consumer<ConfigOnTab> consumer)
    {
        tab.getTabbedExpandedConfigs((c) -> this.addConfigIfHasStatusWidgetFactory(c, consumer));
    }

    protected void addConfigIfHasStatusWidgetFactory(ConfigOnTab cfg, Consumer<ConfigOnTab> consumer)
    {
        if (Registry.CONFIG_STATUS_WIDGET.getConfigStatusWidgetFactory(cfg.config) != null)
        {
            consumer.accept(cfg);
        }
    }
}
