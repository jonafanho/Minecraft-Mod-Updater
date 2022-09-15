package updater.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import updater.mappings.ScreenMapper;
import updater.mappings.Text;
import updater.mappings.UtilitiesClient;

import java.util.function.Function;

public class AddFromLinkScreen extends ScreenMapper implements IGui {

	private String message = "";

	private final ConfigScreen configScreen;
	private final EditBox textFieldUrl;
	private final Button buttonAdd;
	private final Component mainText;
	private final String[] extraText;

	public AddFromLinkScreen(ConfigScreen configScreen, Function<String, String> onAdd, Component mainText, String... extraText) {
		super(Text.literal(""));
		this.configScreen = configScreen;
		this.mainText = mainText;
		this.extraText = extraText;

		textFieldUrl = new EditBox(Minecraft.getInstance().font, 0, 0, 0, SQUARE_SIZE, Text.literal(""));
		buttonAdd = new Button(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.updater.add"), button -> {
			final String url = textFieldUrl.getValue();
			if (!url.isEmpty()) {
				final String result = onAdd.apply(url);
				textFieldUrl.setValue("");
				if (result == null) {
					onClose();
				} else {
					message = result;
				}
			}
		});
	}

	@Override
	protected void init() {
		super.init();

		final int yStart = SQUARE_SIZE + (TEXT_HEIGHT + TEXT_PADDING) * (1 + extraText.length) + TEXT_FIELD_PADDING / 2;
		IGui.setPositionAndWidth(textFieldUrl, SQUARE_SIZE + TEXT_FIELD_PADDING / 2, yStart, width - SQUARE_SIZE * 5 - TEXT_FIELD_PADDING);
		IGui.setPositionAndWidth(buttonAdd, width - SQUARE_SIZE * 4, yStart, SQUARE_SIZE * 3);

		textFieldUrl.setResponder(text -> setAddButtonActive());
		textFieldUrl.setMaxLength(2048);
		setAddButtonActive();

		addDrawableChild(textFieldUrl);
		addDrawableChild(buttonAdd);
	}

	@Override
	public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
		try {
			renderBackground(matrices);
			Gui.drawCenteredString(matrices, font, mainText, width / 2, SQUARE_SIZE, ARGB_WHITE);
			font.drawShadow(matrices, message, SQUARE_SIZE, SQUARE_SIZE * 2 + (TEXT_HEIGHT + TEXT_PADDING) * (1 + extraText.length) + TEXT_PADDING + TEXT_FIELD_PADDING, ARGB_WHITE);
			for (int i = 0; i < extraText.length; i++) {
				font.drawShadow(matrices, extraText[i], SQUARE_SIZE, SQUARE_SIZE + (TEXT_HEIGHT + TEXT_PADDING) * (1 + i), ARGB_WHITE);
			}
			super.render(matrices, mouseX, mouseY, delta);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void tick() {
		textFieldUrl.tick();
	}

	@Override
	public void onClose() {
		super.onClose();
		if (minecraft != null) {
			UtilitiesClient.setScreen(minecraft, configScreen);
		}
	}

	private void setAddButtonActive() {
		buttonAdd.active = !textFieldUrl.getValue().isEmpty();
		if (buttonAdd.active) {
			message = "";
		}
	}
}
