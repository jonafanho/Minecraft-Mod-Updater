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

public abstract class AddFromLinkScreen extends ScreenMapper implements IGui {

	private Component message = null;

	private final ConfigScreen configScreen;
	private final EditBox textField;
	private final Button buttonAdd;
	private final Component mainText;
	private final String[] extraText;

	public AddFromLinkScreen(ConfigScreen configScreen, boolean clearTextBoxAfterSearching, Component mainText, Component buttonText, String... extraText) {
		super(Text.literal(""));
		this.configScreen = configScreen;
		this.mainText = mainText;
		this.extraText = extraText;

		textField = new EditBox(Minecraft.getInstance().font, 0, 0, 0, SQUARE_SIZE, Text.literal(""));
		buttonAdd = new Button(0, 0, 0, SQUARE_SIZE, buttonText, button -> {
			final String text = textField.getValue();
			if (!text.isEmpty()) {
				if (clearTextBoxAfterSearching) {
					textField.setValue("");
				}
				onClickBeforeThread();
				button.active = false;
				textField.active = false;
				message = Text.translatable("gui.updater.please_wait");
				new Thread(() -> onClick(text)).start();
			}
		});
	}

	@Override
	protected void init() {
		super.init();

		final int yStart = getYOffset() - TEXT_PADDING - SQUARE_SIZE - TEXT_FIELD_PADDING / 2;
		IGui.setPositionAndWidth(textField, SQUARE_SIZE + TEXT_FIELD_PADDING / 2, yStart, width - SQUARE_SIZE * 5 - TEXT_FIELD_PADDING);
		IGui.setPositionAndWidth(buttonAdd, width - SQUARE_SIZE * 4, yStart, SQUARE_SIZE * 3);

		textField.setResponder(text -> setAddButtonActive());
		textField.setMaxLength(2048);
		setAddButtonActive();

		addDrawableChild(textField);
		addDrawableChild(buttonAdd);
	}

	@Override
	public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
		try {
			renderBackground(matrices);
			Gui.drawCenteredString(matrices, font, mainText, width / 2, SQUARE_SIZE, ARGB_WHITE);
			if (message != null) {
				font.drawShadow(matrices, message, SQUARE_SIZE, getYOffset(), ARGB_WHITE);
			}
			for (int i = 0; i < extraText.length; i++) {
				font.drawShadow(matrices, extraText[i], SQUARE_SIZE, SQUARE_SIZE + (TEXT_HEIGHT + TEXT_PADDING) * (1 + i), ARGB_WHITE);
			}
			renderAdditional(matrices);
			super.render(matrices, mouseX, mouseY, delta);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void tick() {
		textField.tick();
	}

	@Override
	public void onClose() {
		super.onClose();
		if (minecraft != null) {
			UtilitiesClient.setScreen(minecraft, configScreen);
		}
	}

	protected abstract void onClick(String text);

	protected void onClickBeforeThread() {
	}

	protected void setMessage(Component message) {
		setAddButtonActive();
		this.message = message;
	}

	protected void renderAdditional(PoseStack matrices) {
	}

	protected int getYOffset() {
		return SQUARE_SIZE * 2 + (TEXT_HEIGHT + TEXT_PADDING) * (1 + extraText.length) + TEXT_PADDING + TEXT_FIELD_PADDING;
	}

	private void setAddButtonActive() {
		buttonAdd.active = !textField.getValue().isEmpty();
		if (buttonAdd.active) {
			message = null;
		}
		textField.active = true;
	}
}
