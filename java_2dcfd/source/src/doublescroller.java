import java.awt.Dimension;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.text.DecimalFormat;

/** A component that combines a Scrollbar and a Label, to adjust and display a
 *	parameter of type double.  Not very pretty, robust, or general, but gets the
 *	job done for most purposes, without too much complexity.
 */
class DoubleScroller extends Panel implements AdjustmentListener {

	double theValue, minValue, maxValue, stepSize;	// scrollbar parameters
	String labelText;				// explanatory text to display
	Label theLabel;					// includes explanatory text and the numerical value
	DecimalFormat labelFormat;		// format for displaying the value
	Scrollbar theScrollbar;

	/** Construct a new DoubleScroller given the minimum value, maximum value, step size,
	   initial value, and text label to display to the left of the current value. */
	public DoubleScroller(String label, double min, double max, double step, double initial) {
		minValue = min; maxValue = max; stepSize = step; theValue = initial;
		labelText = label;
		if (decimalPlaces(stepSize) <= 0) {
			labelFormat = new DecimalFormat("0");
		} else {
			StringBuffer pattern = new StringBuffer().append("0.");
			for (int i=0; i<decimalPlaces(stepSize); i++) {pattern.append("0");}
			labelFormat = new DecimalFormat(pattern.toString());
		}
		theLabel = new Label(labelText + labelFormat.format(theValue) + "  ");
			// append a couple of spaces to leave room in case the number grows later
		add(theLabel);			// the label goes on the left
		int scaledInitial = (int) Math.round((initial-min)/step);
		int scaledMax = (int) Math.round((max-min)/step);
		theScrollbar = new Scrollbar(Scrollbar.HORIZONTAL,scaledInitial,1,0,scaledMax+1) {
			public Dimension getPreferredSize() { 	// this anonymous inner class makes
				return new Dimension(100,15);		// the scrollbar 100 pixels long
			}};										// instead of the much smaller default
		add(theScrollbar);		// the scrollbar goes on the right
		theScrollbar.addAdjustmentListener(this);
	}
	
	/* Returns the decimal place of the first sig fig in x. */
	int decimalPlaces(double x) {
		return - (int) Math.floor(Math.log(x)/Math.log(10));
	}

	/* Implements AdjustmentListener to respond to scrollbar adjustment events. */
	public void adjustmentValueChanged(AdjustmentEvent e) {
		int scaledValue = theScrollbar.getValue();
		theValue = scaledValue * stepSize + minValue;
		theLabel.setText(labelText + labelFormat.format(theValue));
	}

	/** Returns the current value of the parameter when asked. */
	public double getValue() {
		return theValue;
	}
}