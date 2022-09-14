package updater.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.util.Mth;
import updater.mappings.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DashboardList implements IGui {

	public int x;
	public int y;
	public int width;
	public int height;

	private final Button buttonPrevPage;
	private final Button buttonNextPage;

	private final Button buttonAction;

	private List<Data> dataList = new ArrayList<>();
	private int hoverIndex, page, totalPages;

	private final int itemHeight;

	public DashboardList(BiConsumer<Data, Integer> onClick, String buttonText, int expectedLines) {
		buttonPrevPage = new Button(0, 0, 0, SQUARE_SIZE, Text.literal("<"), button -> setPage(page - 1));
		buttonNextPage = new Button(0, 0, 0, SQUARE_SIZE, Text.literal(">"), button -> setPage(page + 1));
		buttonAction = new Button(0, 0, 0, SQUARE_SIZE, Text.literal(buttonText), button -> onClick(onClick));
		itemHeight = TEXT_PADDING + (TEXT_HEIGHT + TEXT_PADDING) * expectedLines;
	}

	public void init(Consumer<AbstractWidget> addDrawableChild) {
		IGui.setPositionAndWidth(buttonPrevPage, x, y + TEXT_FIELD_PADDING / 2, SQUARE_SIZE);
		IGui.setPositionAndWidth(buttonNextPage, x + SQUARE_SIZE * 3, y + TEXT_FIELD_PADDING / 2, SQUARE_SIZE);

		buttonAction.visible = false;

		addDrawableChild.accept(buttonPrevPage);
		addDrawableChild.accept(buttonNextPage);
		addDrawableChild.accept(buttonAction);
	}

	public void tick() {
		buttonPrevPage.x = x;
		buttonNextPage.x = x + SQUARE_SIZE * 3;

		final int dataSize = dataList.size();
		totalPages = dataSize == 0 ? 1 : (int) Math.ceil((double) dataSize / itemsToShow());
		setPage(page);
	}

	public void setData(List<Data> dataList) {
		this.dataList = dataList;
	}

	public void render(PoseStack matrices, Font textRenderer) {
		Gui.drawCenteredString(matrices, textRenderer, String.format("%s/%s", page + 1, totalPages), x + SQUARE_SIZE * 2, y + TEXT_PADDING + TEXT_FIELD_PADDING / 2, ARGB_WHITE);
		final int itemsToShow = itemsToShow();
		for (int i = 0; i < itemsToShow; i++) {
			if (i + itemsToShow * page < dataList.size()) {
				final Data data = dataList.get(i + itemsToShow * page);
				for (int j = 0; j < data.text.length; j++) {
					final String drawString = data.text[j];
					final int textStart = TEXT_PADDING * 2;
					final int textWidth = textRenderer.width(drawString);
					final int availableSpace = width - textStart;
					matrices.pushPose();
					matrices.translate(x + textStart, 0, 0);
					if (textWidth > availableSpace) {
						matrices.scale((float) availableSpace / textWidth, 1, 1);
					}
					textRenderer.draw(matrices, drawString, 0, y + SQUARE_SIZE + itemHeight * i + TEXT_PADDING + j * (TEXT_HEIGHT + TEXT_PADDING), ARGB_WHITE);
					matrices.popPose();
				}
			}
		}
	}

	public void mouseMoved(double mouseX, double mouseY) {
		buttonAction.visible = false;

		if (mouseX >= x && mouseX < x + width && mouseY >= y + SQUARE_SIZE && mouseY < y + SQUARE_SIZE + itemHeight * itemsToShow()) {
			hoverIndex = ((int) mouseY - y - SQUARE_SIZE) / itemHeight;
			final int dataSize = dataList.size();
			final int itemsToShow = itemsToShow();
			if (hoverIndex >= 0 && hoverIndex + page * itemsToShow < dataSize) {
				buttonAction.visible = true;
				IGui.setPositionAndWidth(buttonAction, x + width - SQUARE_SIZE, y + hoverIndex * itemHeight + SQUARE_SIZE, SQUARE_SIZE);
			}
		}
	}

	public void mouseScrolled(double mouseX, double mouseY, double amount) {
		if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
			setPage(page + (int) Math.signum(-amount));
		}
	}

	private void setPage(int newPage) {
		page = Mth.clamp(newPage, 0, totalPages - 1);
		buttonPrevPage.visible = page > 0;
		buttonNextPage.visible = page < totalPages - 1;
	}

	private void onClick(BiConsumer<Data, Integer> onClick) {
		final int index = hoverIndex + itemsToShow() * page;
		if (index >= 0 && index < dataList.size()) {
			onClick.accept(dataList.get(index), index);
		}
	}

	private int itemsToShow() {
		return (height - SQUARE_SIZE) / itemHeight;
	}

	public static class Data implements Comparable<Data> {

		private final String[] text;

		public Data(String... text) {
			this.text = text;
		}

		@Override
		public int compareTo(Data data) {
			return String.join(" ", text).compareTo(String.join(" ", data.text));
		}
	}
}
