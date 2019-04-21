TFButton runButton;
ActionButton stepButton;
ActionButton resetFluidButton;
ActionButton drawCircleButton;
ActionButton drawCarButton;
ActionButton resetBarriersButton;

// declare & initialize globally, otherwise causes null pointer exception
Scroller viscosityScroller = new Scroller(20, 100, 0.01, 1.00, "Viscosity = ", 0.02);
Scroller contrastScroller = new Scroller(20, 180, 0, 1.00, "Contrast = ", 0.2);
Scroller inletSpeedScroller = new Scroller(20, 140, 0.00, 1.20, "Inlet Speed = ", 1.0);
Checkbox tracersCheck = new Checkbox(20, 460, "Tracers", false, true);

void setup() {
  size(600, 500);
  noStroke();
  
  // animate as fast as possible
  frameRate(500);
  
  // get fluid flow started
  initFluid();
  initTracers();

  runButton = new TFButton(20, 20, false, "Pause", "Run", 100, 20);
  stepButton = new ActionButton(140, 20, "Step", 100, 20);
  resetFluidButton = new ActionButton(260, 20, "Reset Fluid", 100, 20);
  drawCircleButton = new ActionButton(20, 60, "Draw Circle", 100, 20);
  drawCarButton = new ActionButton(140, 60, "Draw Car", 100, 20);
  resetBarriersButton = new ActionButton(260, 60, "Reset Barriers", 100, 20);
}

void draw() {
  runButton.render();
  stepButton.render();
  resetFluidButton.render();
  drawCircleButton.render();
  drawCarButton.render();
  resetBarriersButton.render();
  viscosityScroller.render();
  contrastScroller.render();
  inletSpeedScroller.render();
  tracersCheck.render();
  drawPlotChoiceSelector();

  if (runButton.b == true) doStep();
  
  paint();
}

void mousePressed() {
  if (mouseY<ydim*pixelsPerSquare && runButton.b==true)
    setBarrier(mouseX/pixelsPerSquare, mouseY/pixelsPerSquare);
  runButton.click();
  tracersCheck.click();

  if (stepButton.isOver() == true) doStep();
  else if (resetFluidButton.isOver() == true) initFluid();
  else if (resetBarriersButton.isOver() == true) clearBarriers();
  else if (drawCarButton.isOver() == true) drawCar();
  else if (drawCircleButton.isOver() == true) makeCircle(20, ydim/2-1, ydim/2-1);
  else updatePlotChoice();
}

void mouseDragged() {
  if (mouseY<ydim*pixelsPerSquare && runButton.b==true)
    setBarrier(mouseX/pixelsPerSquare, mouseY/pixelsPerSquare);
  
  viscosityScroller.update();
  inletSpeedScroller.update();
  contrastScroller.update();
}
