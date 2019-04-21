import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.MemoryImageSource;
import java.text.DecimalFormat;

@SuppressWarnings("serial")
class LatticeBoltzmannDemo extends Canvas implements Runnable, MouseListener, MouseMotionListener {

	// Global variables, starting with the grid size:
	int xdim = 200; // dimensions of lattice
	int ydim = 80;
	int pixelsPerSquare = 3; // for graphics

	// Here are the arrays of densities by velocity, named by velocity directions
	// with north up:
	double[][] n0 = new double[xdim][ydim];
	double[][] nN = new double[xdim][ydim];
	double[][] nS = new double[xdim][ydim];
	double[][] nE = new double[xdim][ydim];
	double[][] nW = new double[xdim][ydim];
	double[][] nNW = new double[xdim][ydim];
	double[][] nNE = new double[xdim][ydim];
	double[][] nSW = new double[xdim][ydim];
	double[][] nSE = new double[xdim][ydim];

	// Other arrays calculated from the above:
	double[][] density = new double[xdim][ydim]; // total density
	double[][] xvel = new double[xdim][ydim]; // macroscopic x velocity
	double[][] yvel = new double[xdim][ydim]; // macroscopic y velocity
	double[][] speed2 = new double[xdim][ydim]; // macroscopic speed squared

	// Boolean array, true at sites that contain barriers:
	boolean[][] barrier = new boolean[xdim][ydim];

	int time = 0; // time in units of the fundamental step size

	// Tracers:
	Checkbox tracersCheck = new Checkbox("Tracers", false);
	int numTracers = 256;
	double[] tracerx = new double[numTracers];
	double[] tracery = new double[numTracers];
	Color tracerColor = new Color(120, 0, 0); // tracer particle color

	// Array of colors for graphics:
	int nColors = 600; // 600 colors total in the gradient
	int[] colorInt = new int[nColors]; // colors stored as integers for MemoryImageSource
	int blackColorInt = Color.HSBtoRGB((float) 0, (float) 1, (float) 0); // an integer to represent the color black
	{
		for (int c = 0; c < nColors; c++) {
			double h = (2.0 / 3) * (1 - c * 1.0 / nColors); // hue from blue->cyan->green->yellow->red
			h += 0.03 * Math.sin(6 * Math.PI * h); // for smoother color gradations
			// shade[c] = Color.getHSBColor((float)h,(float)1,(float)1);
			colorInt[c] = Color.HSBtoRGB((float) h, (float) 1, (float) 1); // store each color as an integer
		}
	}

	// Image-related objects for sophisticated memory image source:
	// int[] iPixels = new int[xdim * ydim];
	// MemoryImageSource iSource = new MemoryImageSource(xdim,ydim,iPixels,0,xdim);
	int[] iPixels = new int[xdim * pixelsPerSquare * ydim * pixelsPerSquare];
	MemoryImageSource iSource = new MemoryImageSource(xdim * pixelsPerSquare, ydim * pixelsPerSquare, iPixels, 0,
			xdim * pixelsPerSquare);

	Image theImage;
	Image scaledImage;

	boolean running = false; // true when the simulation thread is running
	int stepTime = 0; // performance measure: time in ms for a single iteration of the algorithm
	int collideTime = 0;
	int streamTime = 0;
	int paintTime = 0;
	int mouseX, mouseY; // mouse coordinates in grid units
	boolean mouseInCanvas = false;
	boolean mouseDrawBarrier = true; // true when mouse is drawing rather than erasing a barrier
	Cursor crosshairCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);
	Cursor arrowCursor = new Cursor(Cursor.DEFAULT_CURSOR);

	Canvas dataCanvas; // for numerical readouts
	DecimalFormat threePlaces = new DecimalFormat("0.000");
	Button runButton = new Button(" Run ");
	DoubleScroller barrierSizeScroller = new DoubleScroller("Barrier size = ", 1, 50, 1, 20);
	DoubleScroller viscScroller = new DoubleScroller("Viscosity = ", .01, 1, .01, .02);
	DoubleScroller speedScroller = new DoubleScroller("Flow speed = ", 0, 0.12, 0.005, 0.1);
	Checkbox pbcCheck = new Checkbox("PBC", true);
	DoubleScroller contrastScroller = new DoubleScroller("Contrast = ", 1, 100, 1, 20);
	Choice plotChoice = new Choice();

	// calculation short-cuts:
	double FOUR_NINTHS = 4.0 / 9;
	double ONE_NINTH = 1.0 / 9;
	double ONE_THIRTYSIXTH = 1.0 / 36;

	// Constructor method does all the initializations:
	LatticeBoltzmannDemo() {

		initFluid(); // initialize the fluid state
		initTracers(); // place the tracer particles

		// Start the GUI with a Frame and a Panel to hold the Canvas:
		setSize(xdim * pixelsPerSquare, ydim * pixelsPerSquare);
		addMouseListener(this);
		addMouseMotionListener(this);
		Frame theFrame = new Frame("Lattice-Boltzmann Fluid");
		theFrame.setResizable(false);
		theFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0); // exit when user clicks close button
			}
		});
		Panel canvasPanel = new Panel();
		theFrame.add(canvasPanel);
		canvasPanel.add(this);

		// Offscreen image and memory image source:
		iSource.setAnimated(true);
		theImage = createImage(iSource);

		// Add a control panel and data-readout canvas:
		Panel controlPanel = new Panel();
		theFrame.add(controlPanel, BorderLayout.SOUTH);
		controlPanel.setLayout(new GridLayout(0, 1)); // divide controlPanel into equal-height rows
		dataCanvas = new Canvas() {
			public void paint(Graphics g) {
				if (mouseInCanvas) {
					g.drawString("t = " + time + ", x = " + mouseX + ", y = " + mouseY + ", density = "
							+ threePlaces.format(density[mouseX][mouseY]) + ", speed = "
							+ threePlaces.format(Math.sqrt(speed2[mouseX][mouseY])) + ", vx = "
							+ threePlaces.format(xvel[mouseX][mouseY]) + ", vy = "
							+ threePlaces.format(yvel[mouseX][mouseY])
					/*
					 * ", n0 = " + threePlaces.format(n0[mouseX][mouseY]) + ", nE = " +
					 * threePlaces.format(nE[mouseX][mouseY]) + ", nW = " +
					 * threePlaces.format(nW[mouseX][mouseY]) + ", nN = " +
					 * threePlaces.format(nN[mouseX][mouseY]) + ", nS = " +
					 * threePlaces.format(nS[mouseX][mouseY]) + ", nNE = " +
					 * threePlaces.format(nNE[mouseX][mouseY]) + ", nNW = " +
					 * threePlaces.format(nNW[mouseX][mouseY]) + ", nSE = " +
					 * threePlaces.format(nSE[mouseX][mouseY]) + ", nSW = " +
					 * threePlaces.format(nSW[mouseX][mouseY])
					 */
					, 10, 15);
				} else {
					// performance metrics
					g.drawString("t = " + time + "; Calculation: " + stepTime + " ms; collision: " + collideTime
							+ " ms; stream: " + streamTime + " ms; paint: " + paintTime + " ms", 10, 15);
				}
			}
		};
		controlPanel.add(dataCanvas);

		// Sub-panel for buttons:
		Panel cPanel1 = new Panel();
		controlPanel.add(cPanel1);
		cPanel1.add(runButton);
		runButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				running = !running;
				if (running)
					runButton.setLabel("Pause");
				else
					runButton.setLabel("Run");
			}
		});
		Button stepButton = new Button("Step");
		cPanel1.add(stepButton);
		stepButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doStep();
				// System.out.println(density[10][10]);
			}
		});
		Button resetButton = new Button("Reset fluid");
		cPanel1.add(resetButton);
		resetButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				initFluid();
				repaint();
			}
		});

		Panel cPanel2 = new Panel();
		controlPanel.add(cPanel2);
		cPanel2.add(barrierSizeScroller);
		Button squareButton = new Button("Draw Square");
		cPanel2.add(squareButton);
		squareButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearBarriers();
				makeRect(ydim / 2 - 1, ydim / 2 - 1, (int) Math.round(barrierSizeScroller.getValue()),
						(int) Math.round(barrierSizeScroller.getValue()));
			}
		});
		Button circleButton = new Button("Draw Circle");
		cPanel2.add(circleButton);
		circleButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearBarriers();
				makeCircle((int) Math.round(barrierSizeScroller.getValue()), ydim / 2 - 1, ydim / 2 - 1);
			}
		});
		Button carButton = new Button("Draw Car");
		cPanel2.add(carButton);
		carButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearBarriers();
				makeCar();
			}
		});
		Button clearButton = new Button("Clear barriers");
		cPanel2.add(clearButton);
		clearButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearBarriers();
			}
		});

		// Sub-panel for physical settings:
		Panel cPanel3 = new Panel();
		controlPanel.add(cPanel3);
		cPanel3.add(viscScroller);
		cPanel3.add(speedScroller);
		// cPanel2.add(pbcCheck);

		// Sub-panel for visualization stuff:
		Panel cPanel4 = new Panel();
		controlPanel.add(cPanel4);
		cPanel4.add(plotChoice);
		plotChoice.add("Plot Speed");
		plotChoice.add("Plot Velocity X");
		plotChoice.add("Plot Velocity Y");
		plotChoice.add("Plot Curl");
		plotChoice.add("Plot Density");
		plotChoice.select(0); // initially plot the speed
		cPanel4.add(contrastScroller);
		cPanel4.add(tracersCheck);
		tracersCheck.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (tracersCheck.getState())
					initTracers();
				repaint();
			}
		});

		// GUI is complete so pack the frame and show it:
		theFrame.pack();
		theFrame.setVisible(true);

		makeRect(40, 40, 20, 20); // start with a square barrier

		// Now start the simulation thread:
		Thread simThread = new Thread(this);
		simThread.start();
	} // end of constructor method

	// Initialize the fluid with density 1 and user-chosen speed in x direction:
	synchronized void initFluid() {
		double v = speedScroller.getValue();
		for (int x = 0; x < xdim; x++) {
			for (int y = 0; y < ydim; y++) {
				if (barrier[x][y]) {
					zeroSite(x, y);
				} else {
					n0[x][y] = FOUR_NINTHS * (1 - 1.5 * v * v);
					nE[x][y] = ONE_NINTH * (1 + 3 * v + 3 * v * v);
					nW[x][y] = ONE_NINTH * (1 - 3 * v + 3 * v * v);
					nN[x][y] = ONE_NINTH * (1 - 1.5 * v * v);
					nS[x][y] = ONE_NINTH * (1 - 1.5 * v * v);
					nNE[x][y] = ONE_THIRTYSIXTH * (1 + 3 * v + 3 * v * v);
					nSE[x][y] = ONE_THIRTYSIXTH * (1 + 3 * v + 3 * v * v);
					nNW[x][y] = ONE_THIRTYSIXTH * (1 - 3 * v + 3 * v * v);
					nSW[x][y] = ONE_THIRTYSIXTH * (1 - 3 * v + 3 * v * v);
					density[x][y] = 1;
					xvel[x][y] = v;
					yvel[x][y] = 0;
					speed2[x][y] = v * v;
				}
			}
		}
		time = 0; // reset time variable
	}

	// Clear all the user-drawn barriers:
	synchronized void clearBarriers() {
		for (int x = 1; x < xdim - 1; x++) {
			for (int y = 1; y < ydim - 1; y++) {
				if (barrier[x][y]) {
					barrier[x][y] = false;
					n0[x][y] = 1;
					density[x][y] = 1;
					speed2[x][y] = 0;
				}
			}
		}
	}

	// Create a rectangle of given base and height
	synchronized void makeRect(int _x, int _y, int _width, int _height) {
		// rectangle is fixed about the center
		mouseDrawBarrier = true;
		for (int x = _x - _width / 2; x < _x + _width / 2; x++) {
			for (int y = _y - _height / 2; y < _y + _height / 2; y++) {
				drawBarrier(x, y);
			}
		}
	}

	// Create a circular barrier of given diameter:
	synchronized void makeCircle(int diameter, double _centerX, double _centerY) {
		mouseDrawBarrier = true;
		double radius = (diameter - 1) / 2.0; // 1->0, 2->.5, 3->1, 4->1.5, etc.
		double centerY = _centerY;
		if (diameter % 2 == 0)
			centerY -= 0.5; // shift down a bit if diameter is an even number
		double centerX = _centerX;
		for (int radii = 0; radii < radius; radii++) {
			for (double theta = 0; theta < 2 * Math.PI; theta += 0.1 / radius) {
				int x = (int) Math.round(centerX + radii * Math.cos(theta));
				int y = (int) Math.round(centerY + radii * Math.sin(theta));
				drawBarrier(x, y);
				if (radius > 1) {
					x = (int) Math.round(centerX + (radii - 0.5) * Math.cos(theta));
					y = (int) Math.round(centerY + (radii - 0.5) * Math.sin(theta));
					drawBarrier(x, y);
				}
			}
		}
	}

	// Draw a boxy car:
	synchronized void makeCar() {
		mouseDrawBarrier = true;
		makeRect(100, 22, 100, 20);
		makeRect(100, 38, 44, 12);
		makeCircle(20, 70, 12);
		makeCircle(20, 130, 12);
	}

	// Handy method to set all densities at a site to zero:
	void zeroSite(int x, int y) {
		n0[x][y] = 0;
		nE[x][y] = 0;
		nW[x][y] = 0;
		nN[x][y] = 0;
		nS[x][y] = 0;
		nNE[x][y] = 0;
		nNW[x][y] = 0;
		nSE[x][y] = 0;
		nSW[x][y] = 0;
		xvel[x][y] = 0;
		yvel[x][y] = 0;
		speed2[x][y] = 0;
	}

	// Initialize tracer positions, equally spaced:
	void initTracers() {
		int nRow = (int) Math.sqrt(numTracers); // number of tracers in a row
		numTracers = nRow * nRow; // force nTracers to be a perfect square
		double dx = xdim * 1.0 / nRow;
		double dy = ydim * 1.0 / nRow;
		int next = 0;
		for (int x = 0; x < nRow; x++) {
			for (int y = 0; y < nRow; y++) {
				tracerx[next] = (x + 0.5) * dx;
				tracery[next] = (y + 0.5) * dy;
				next++;
			}
		}
	}

	// Run the simulation (called from separate thread):
	public void run() {
		while (true) {
			if (running) {
				for (int s = 0; s < 10; s++)
					doStep();
				try { Thread.sleep(1); } catch (InterruptedException e) {}
				repaint();
			} else try { Thread.sleep(200); } catch (InterruptedException e) {}
			repaint(); // repeated painting when not running uses resources but is handy for graphics adjustments
		}
	}

	// Execute a single step of the algorithm:
	// Times are on 3.06 GHz iMac, Java 6. On 2.4GHz MacBook Pro, all times are
	// about 30% longer.
	synchronized void doStep() {
		long startTime = System.currentTimeMillis();
		long forceTime = System.currentTimeMillis();
		collide();
		long afterCollideTime = System.currentTimeMillis();
		collideTime = (int) (afterCollideTime - forceTime); // 23-24 ms for 600x600 grid
		stream();
		streamTime = (int) (System.currentTimeMillis() - afterCollideTime); // 9-10 ms for 600x600 grid
		bounce();
		if (tracersCheck.getState())
			moveTracers();
		stepTime = (int) (System.currentTimeMillis() - startTime); // 33-35 ms for 600x600 grid
		time++;
		dataCanvas.repaint();
	}

	// Collide particles within each cell. Adapted from Wagner's D2Q9 code.
	void collide() {
		double n, one9thn, one36thn, vx, vy, vx2, vy2, vx3, vy3, vxvy2, v2, v215;
		double omega = 1 / (3 * viscScroller.getValue() + 0.5); // reciprocal of tau, the relaxation time
		for (int x = 0; x < xdim; x++) {
			for (int y = 0; y < ydim; y++) {
				if (!barrier[x][y]) {
					n = n0[x][y] + nN[x][y] + nS[x][y] + nE[x][y] + nW[x][y] + nNW[x][y] + nNE[x][y] + nSW[x][y]
							+ nSE[x][y];
					density[x][y] = n; // macroscopic density may be needed for plotting
					one9thn = ONE_NINTH * n;
					one36thn = ONE_THIRTYSIXTH * n;
					if (n > 0) {
						vx = (nE[x][y] + nNE[x][y] + nSE[x][y] - nW[x][y] - nNW[x][y] - nSW[x][y]) / n;
					} else
						vx = 0;
					xvel[x][y] = vx; // may be needed for plotting
					if (n > 0) {
						vy = (nN[x][y] + nNE[x][y] + nNW[x][y] - nS[x][y] - nSE[x][y] - nSW[x][y]) / n;
					} else
						vy = 0;
					yvel[x][y] = vy; // may be needed for plotting
					vx3 = 3 * vx;
					vy3 = 3 * vy;
					vx2 = vx * vx;
					vy2 = vy * vy;
					vxvy2 = 2 * vx * vy;
					v2 = vx2 + vy2;
					speed2[x][y] = v2; // may be needed for plotting
					v215 = 1.5 * v2;
					n0[x][y] += omega * (FOUR_NINTHS * n * (1 - v215) - n0[x][y]);
					nE[x][y] += omega * (one9thn * (1 + vx3 + 4.5 * vx2 - v215) - nE[x][y]);
					nW[x][y] += omega * (one9thn * (1 - vx3 + 4.5 * vx2 - v215) - nW[x][y]);
					nN[x][y] += omega * (one9thn * (1 + vy3 + 4.5 * vy2 - v215) - nN[x][y]);
					nS[x][y] += omega * (one9thn * (1 - vy3 + 4.5 * vy2 - v215) - nS[x][y]);
					nNE[x][y] += omega * (one36thn * (1 + vx3 + vy3 + 4.5 * (v2 + vxvy2) - v215) - nNE[x][y]);
					nNW[x][y] += omega * (one36thn * (1 - vx3 + vy3 + 4.5 * (v2 - vxvy2) - v215) - nNW[x][y]);
					nSE[x][y] += omega * (one36thn * (1 + vx3 - vy3 + 4.5 * (v2 - vxvy2) - v215) - nSE[x][y]);
					nSW[x][y] += omega * (one36thn * (1 - vx3 - vy3 + 4.5 * (v2 + vxvy2) - v215) - nSW[x][y]);
				}
			}
		}
	}

	// Stream particles into neighboring cells:
	void stream() {
		for (int x = 0; x < xdim - 1; x++) { // first start in NW corner...
			for (int y = ydim - 1; y > 0; y--) {
				nN[x][y] = nN[x][y - 1]; // move the north-moving particles
				nNW[x][y] = nNW[x + 1][y - 1]; // and the northwest-moving particles
			}
		}
		for (int x = xdim - 1; x > 0; x--) { // now start in NE corner...
			for (int y = ydim - 1; y > 0; y--) {
				nE[x][y] = nE[x - 1][y]; // move the east-moving particles
				nNE[x][y] = nNE[x - 1][y - 1]; // and the northeast-moving particles
			}
		}
		for (int x = xdim - 1; x > 0; x--) { // now start in SE corner...
			for (int y = 0; y < ydim - 1; y++) {
				nS[x][y] = nS[x][y + 1]; // move the south-moving particles
				nSE[x][y] = nSE[x - 1][y + 1]; // and the southeast-moving particles
			}
		}
		for (int x = 0; x < xdim - 1; x++) { // now start in the SW corner...
			for (int y = 0; y < ydim - 1; y++) {
				nW[x][y] = nW[x + 1][y]; // move the west-moving particles
				nSW[x][y] = nSW[x + 1][y + 1]; // and the southwest-moving particles
			}
		}
		// We missed a few at the left and right edges:
		for (int y = 0; y < ydim - 1; y++) {
			nS[0][y] = nS[0][y + 1];
		}
		for (int y = ydim - 1; y > 0; y--) {
			nN[xdim - 1][y] = nN[xdim - 1][y - 1];
		}
		// Now handle left boundary as in Pullan's example code:
		// Stream particles in from the non-existent space to the left, with the
		// user-determined speed:
		double v = speedScroller.getValue();
		for (int y = 0; y < ydim; y++) {
			if (!barrier[0][y]) {
				nE[0][y] = ONE_NINTH * (1 + 3 * v + 3 * v * v);
				nNE[0][y] = ONE_THIRTYSIXTH * (1 + 3 * v + 3 * v * v);
				nSE[0][y] = ONE_THIRTYSIXTH * (1 + 3 * v + 3 * v * v);
			}
		}
		// Try the same thing at the right edge and see if it works:
		for (int y = 0; y < ydim; y++) {
			if (!barrier[0][y]) {
				nW[xdim - 1][y] = ONE_NINTH * (1 - 3 * v + 3 * v * v);
				nNW[xdim - 1][y] = ONE_THIRTYSIXTH * (1 - 3 * v + 3 * v * v);
				nSW[xdim - 1][y] = ONE_THIRTYSIXTH * (1 - 3 * v + 3 * v * v);
			}
		}
		// Now handle top and bottom edges:
		for (int x = 0; x < xdim; x++) {
			n0[x][0] = FOUR_NINTHS * (1 - 1.5 * v * v);
			nE[x][0] = ONE_NINTH * (1 + 3 * v + 3 * v * v);
			nW[x][0] = ONE_NINTH * (1 - 3 * v + 3 * v * v);
			nN[x][0] = ONE_NINTH * (1 - 1.5 * v * v);
			nS[x][0] = ONE_NINTH * (1 - 1.5 * v * v);
			nNE[x][0] = ONE_THIRTYSIXTH * (1 + 3 * v + 3 * v * v);
			nSE[x][0] = ONE_THIRTYSIXTH * (1 + 3 * v + 3 * v * v);
			nNW[x][0] = ONE_THIRTYSIXTH * (1 - 3 * v + 3 * v * v);
			nSW[x][0] = ONE_THIRTYSIXTH * (1 - 3 * v + 3 * v * v);
			n0[x][ydim - 1] = FOUR_NINTHS * (1 - 1.5 * v * v);
			nE[x][ydim - 1] = ONE_NINTH * (1 + 3 * v + 3 * v * v);
			nW[x][ydim - 1] = ONE_NINTH * (1 - 3 * v + 3 * v * v);
			nN[x][ydim - 1] = ONE_NINTH * (1 - 1.5 * v * v);
			nS[x][ydim - 1] = ONE_NINTH * (1 - 1.5 * v * v);
			nNE[x][ydim - 1] = ONE_THIRTYSIXTH * (1 + 3 * v + 3 * v * v);
			nSE[x][ydim - 1] = ONE_THIRTYSIXTH * (1 + 3 * v + 3 * v * v);
			nNW[x][ydim - 1] = ONE_THIRTYSIXTH * (1 - 3 * v + 3 * v * v);
			nSW[x][ydim - 1] = ONE_THIRTYSIXTH * (1 - 3 * v + 3 * v * v);
		}
	}

	// Bounce particles off of barriers:
	// (The ifs are needed to prevent array index out of bounds errors.
	// Could handle edges separately to avoid this.)
	void bounce() {
		for (int x = 0; x < xdim; x++) {
			for (int y = 0; y < ydim; y++) {
				if (barrier[x][y]) {
					if (nN[x][y] > 0) {
						nS[x][y - 1] += nN[x][y];
						nN[x][y] = 0;
					}
					if (nS[x][y] > 0) {
						nN[x][y + 1] += nS[x][y];
						nS[x][y] = 0;
					}
					if (nE[x][y] > 0) {
						nW[x - 1][y] += nE[x][y];
						nE[x][y] = 0;
					}
					if (nW[x][y] > 0) {
						nE[x + 1][y] += nW[x][y];
						nW[x][y] = 0;
					}
					if (nNW[x][y] > 0) {
						nSE[x + 1][y - 1] += nNW[x][y];
						nNW[x][y] = 0;
					}
					if (nNE[x][y] > 0) {
						nSW[x - 1][y - 1] += nNE[x][y];
						nNE[x][y] = 0;
					}
					if (nSW[x][y] > 0) {
						nNE[x + 1][y + 1] += nSW[x][y];
						nSW[x][y] = 0;
					}
					if (nSE[x][y] > 0) {
						nNW[x - 1][y + 1] += nSE[x][y];
						nSE[x][y] = 0;
					}
				}
			}
		}
	}

	// Move the tracer particles according to the macroscopic velocity:
	void moveTracers() {
		for (int t = 0; t < numTracers; t++) {
			int x = (int) tracerx[t]; // convert coordinates to integers
			int y = (int) tracery[t];
			tracerx[t] += xvel[x][y]; // move 'em along the flow
			tracery[t] += yvel[x][y];
			if (tracerx[t] < 0) tracerx[t] = 0; // don't let 'em go out of bounds
			if (tracerx[t] >= xdim) tracerx[t] = 0; // recycle when it exits to the right
			if (tracery[t] < 0) tracery[t] = 0;
			if (tracery[t] >= ydim) tracery[t] = ydim - 1;
		}
	}

	// Compute the curl of the velocity field, paying special attention to edges:
	double[][] curl = new double[xdim][ydim];

	void computeCurl() {
		for (int x = 1; x < xdim - 1; x++) {
			for (int y = 1; y < ydim - 1; y++) {
				curl[x][y] = (yvel[x + 1][y] - yvel[x - 1][y]) - (xvel[x][y + 1] - xvel[x][y - 1]);
			}
		}
		for (int y = 1; y < ydim - 1; y++) {
			curl[0][y] = 2 * (yvel[1][y] - yvel[0][y]) - (xvel[0][y + 1] - xvel[0][y - 1]);
			curl[xdim - 1][y] = 2 * (yvel[xdim - 1][y] - yvel[xdim - 2][y]) - (xvel[xdim - 1][y + 1] - xvel[xdim - 1][y - 1]);
		}
	}

	// Override update method to skip drawing background color:
	public void update(Graphics g) {
		paint(g);
	}

	// Paint method draws everything:
	public void paint(Graphics g) {
		long startTime = System.currentTimeMillis();
		int plotType = plotChoice.getSelectedIndex(); // 0 for speed, 1 for vx, 2 for vy, 3 for curl, 4 for density
		if (plotType == 3) computeCurl();
		double contrast = contrastScroller.getValue(); // multiplicative factor for colors
		int colorIndex; // index into array of colors
		int theColor; // color of a square, stored as an integer
		int pIndex = 0; // index into pixel array
		for (int y = ydim - 1; y >= 0; y--) { // note that we loop over y (row number) first, high to low
			for (int x = 0; x < xdim; x++) {
				if (barrier[x][y]) { theColor = blackColorInt; }
				else {
					// Set the color of each pixel
					if (plotType == 0) { 		colorIndex = (int) (Math.sqrt(speed2[x][y]) * nColors * contrast * 0.2); }
					else if (plotType == 1) { colorIndex = (int) (nColors * (0.5 + xvel[x][y] * contrast * 0.2)); }
					else if (plotType == 2) { colorIndex = (int) (nColors * (0.5 + yvel[x][y] * contrast * 0.2)); }
					else if (plotType == 3) { colorIndex = (int) (nColors * (0.5 + curl[x][y] * contrast * 0.3)); } 
					else { 					colorIndex = (int) (nColors * (0.5 + (density[x][y] - 1) * contrast * 0.3)); }
					if (colorIndex < 0) colorIndex = 0;
					if (colorIndex >= nColors) colorIndex = nColors - 1;
					theColor = colorInt[colorIndex];
				}
				// Now draw a square the hard way, one pixel at a time...
				// (We could make the memory image one pixel per square and use drawImage to
				// enlarge it,
				// but on Java 1.5 for Mac and perhaps others, this blurs the image.)
				for (int j = 0; j < pixelsPerSquare; j++) { // loop over rows of pixels
					for (int i = 0; i < pixelsPerSquare; i++) { // loop over columns of pixels
						iPixels[pIndex] = theColor;
						pIndex++;
					}
					pIndex += (xdim - 1) * pixelsPerSquare; // go to the next row
				}
				pIndex += pixelsPerSquare * (1 - xdim * pixelsPerSquare); // get ready for next square
				// g.fillRect(x*pixelsPerSquare,(ydim-y-1)*pixelsPerSquare,pixelsPerSquare,pixelsPerSquare);
			}
			pIndex -= xdim * pixelsPerSquare * (1 - pixelsPerSquare); // get ready for next grid row
		}
		iSource.newPixels(0, 0, xdim * pixelsPerSquare, ydim * pixelsPerSquare); // inform AWT that memory image has
																					// changed
		g.drawImage(theImage, 0, 0, null); // blast the image to the screen

		// Now draw the tracer particles:
		if (tracersCheck.getState()) {
			g.setColor(tracerColor);
			for (int tracer = 0; tracer < numTracers; tracer++) {
				int tx = (int) Math.round((tracerx[tracer] + 0.5) * pixelsPerSquare);
				int ty = (int) Math.round((ydim - (tracery[tracer] + 0.5)) * pixelsPerSquare);
				g.fillRect(tx - 1, ty - 2, 3, 3);
			}
		}
		paintTime = (int) (System.currentTimeMillis() - startTime);
	} // end of paint method

	// Handle initial mouse press:
	public void mousePressed(MouseEvent e) {
		mouseX = e.getX() / pixelsPerSquare;
		mouseY = ydim - 1 - e.getY() / pixelsPerSquare;
		if (mouseX < 1 || mouseX >= xdim - 1 || mouseY < 1 || mouseY >= ydim - 1) return;
		if (barrier[mouseX][mouseY]) mouseDrawBarrier = false;
		else mouseDrawBarrier = true;
		drawBarrier(mouseX, mouseY);
	}

	// Handle mouse drag to a different location:
	public void mouseDragged(MouseEvent e) {
		mouseX = e.getX() / pixelsPerSquare;
		mouseY = ydim - 1 - e.getY() / pixelsPerSquare;
		if (mouseX < 1 || mouseX >= xdim - 1 || mouseY < 1 || mouseY >= ydim - 1) return;
		drawBarrier(mouseX, mouseY);
	}

	// A grid point has been clicked or dragged; create or erase a barrier
	// accordingly:
	void drawBarrier(int x, int y) {
		if (mouseDrawBarrier) {
			barrier[x][y] = true;
			zeroSite(x, y); // set all densities to zero if drawing a barrier here
		} else {
			if (barrier[x][y]) { // don't erase unless there's actually a barrier here
				barrier[x][y] = false;
				n0[x][y] = 1; // place some motionless fluid here with density 1
				density[x][y] = 1;
				speed2[x][y] = 0; // paint method needs to know that speed is zero
			}
		}
		repaint();
	}

	// Mouse has entered canvas so get location for display on dataCanvas:
	public void mouseEntered(MouseEvent e) {
		setCursor(crosshairCursor);
		mouseInCanvas = true;
		mouseX = e.getX() / pixelsPerSquare;
		mouseY = ydim - 1 - e.getY() / pixelsPerSquare;
		dataCanvas.repaint();
	}

	// Mouse has exited canvas:
	public void mouseExited(MouseEvent e) {
		setCursor(arrowCursor);
		mouseInCanvas = false;
		dataCanvas.repaint();
	}

	// Mouse has moved within canvas so update location for display on dataCanvas:
	public void mouseMoved(MouseEvent e) {
		mouseX = e.getX() / pixelsPerSquare;
		mouseY = ydim - 1 - e.getY() / pixelsPerSquare;
		dataCanvas.repaint();
	}

	public void mouseClicked(MouseEvent e) {}

	public void mouseReleased(MouseEvent e) {}

	public static void main(String[] arg) {
		new LatticeBoltzmannDemo();
	}
}