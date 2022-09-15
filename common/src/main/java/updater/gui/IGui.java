package updater.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.util.Mth;

public interface IGui {

	int SQUARE_SIZE = 20;
	int TEXT_HEIGHT = 8;
	int TEXT_PADDING = 6;
	int TEXT_FIELD_PADDING = 4;
	int ARGB_WHITE = 0xFFFFFFFF;
	int ARGB_BLACK = 0xFF000000;

	static void setPositionAndWidth(AbstractWidget widget, int x, int y, int widgetWidth) {
		widget.x = x;
		widget.y = y;
		widget.setWidth(Mth.clamp(widgetWidth, 0, 380));
	}
}
