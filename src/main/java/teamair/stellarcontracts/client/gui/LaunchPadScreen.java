package teamair.stellarcontracts.client.gui;

import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.mutable.MutableInt;
import spinnery.client.screen.BaseContainerScreen;
import spinnery.widget.WAbstractWidget;
import spinnery.widget.WInterface;
import spinnery.widget.WStaticImage;
import spinnery.widget.WStaticText;
import spinnery.widget.api.Position;
import spinnery.widget.api.Size;
import teamair.stellarcontracts.StellarContracts;
import teamair.stellarcontracts.block.entity.LaunchPadBlockEntity;
import teamair.stellarcontracts.client.widget.WBuildBar;
import teamair.stellarcontracts.client.widget.WTexturedPanel;
import teamair.stellarcontracts.client.widget.WTexturedSlot;
import teamair.stellarcontracts.container.LaunchPadContainer;

public class LaunchPadScreen extends BaseContainerScreen<LaunchPadContainer> {
    private static final Identifier TEXTURE = StellarContracts.id("textures/gui/launch_pad_background.png");
    private static final Identifier ERROR_TEXTURE = StellarContracts.id("textures/gui/cross.png");
    private static final Text TOP = new TranslatableText("texts.stellar_contracts.rocket_crate_top_name");
    private static final Text BOTTOM = new TranslatableText("texts.stellar_contracts.rocket_crate_bottom_name");

    private final WBuildBar progressBar;
    private final WStaticImage errorIcon;

    public LaunchPadScreen(LaunchPadContainer linkedContainer) {
        super(TOP, linkedContainer, linkedContainer.player);

        WInterface mainInterface = getInterface();
        WTexturedPanel mainPanel = mainInterface.createChild(WTexturedPanel::new,
            Position.of(0, 0, 0),
            Size.of(175, 220)
        ).setParent(mainInterface);

        mainPanel.setTexture(TEXTURE);
        mainPanel.setOnAlign(WAbstractWidget::center);
        mainPanel.center();

        progressBar = new WBuildBar()
            .setLimit(new MutableInt(LaunchPadBlockEntity.MAX_BUILD_PROGRESS))
            .setProgress(new MutableInt(linkedContainer.launchPad.getBuildProgress()))
            .setSize(Size.of(150, 5))
            .setPosition(Position.of(mainPanel, 13, 111));
        mainPanel.add(progressBar);

        errorIcon = new WStaticImage()
            .setTexture(ERROR_TEXTURE)
            .setHidden(true)
            .setSize(Size.of(16, 16))
            .setPosition(Position.of(mainPanel, 32, 60));
        mainPanel.add(errorIcon);

        WTexturedSlot.addTArray(
            Position.of(mainPanel, 77, 30),
            Size.of(18, 18),
            mainPanel,
            0, 1, 1, 4,
            true
        );

        mainPanel.add(new WStaticText()
            .setText(BOTTOM)
            .setPosition(Position.of(mainPanel, 8, 124)));

        WTexturedSlot.addTPlayerInventory(
            Position.of(mainPanel, 7, 137),
            Size.of(18, 18),
            mainPanel
        );

        mainInterface.add(mainPanel);
        mainInterface.setBlurred(true);
    }

    @Override
    public void tick() {
        ArrayPropertyDelegate syncProperties = getContainer().syncProperties;
        errorIcon.setHidden(syncProperties.get(0) != 0);
        progressBar.getProgress().setValue(syncProperties.get(1));
        super.tick();
    }
}
