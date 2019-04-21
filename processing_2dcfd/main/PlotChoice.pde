Checkbox speedCheck = new Checkbox(380, 60, "Speed", true, false);
Checkbox vxCheck = new Checkbox(380, 100, "X Velocity", false, false);
Checkbox vyCheck = new Checkbox(380, 140, "Y Velocity", false, false);
Checkbox curlCheck = new Checkbox(380, 180, "Curl", false, false);
Checkbox densityCheck = new Checkbox(380, 220, "Density", false, false);

void drawPlotChoiceSelector() {
  textAlign(LEFT, CENTER);
  fill(240);
  rect(380, 20+ydim*3, 200, 220);
  fill(10);
  text("Choose which variable to plot:", 380, 30+ydim*3);

  speedCheck.render();
  vxCheck.render();
  vyCheck.render();
  curlCheck.render();
  densityCheck.render();
}

void updatePlotChoice() {
  if (speedCheck.isOver() == true) { 
    speedCheck.setValue(true);    
    vxCheck.setValue(false);   
    vyCheck.setValue(false);   
    curlCheck.setValue(false);   
    densityCheck.setValue(false);
  } else if (vxCheck.isOver() == true) { 
    speedCheck.setValue(false);   
    vxCheck.setValue(true);    
    vyCheck.setValue(false);   
    curlCheck.setValue(false);   
    densityCheck.setValue(false);
  } else if (vyCheck.isOver() == true) { 
    speedCheck.setValue(false);   
    vxCheck.setValue(false);   
    vyCheck.setValue(true);    
    curlCheck.setValue(false);   
    densityCheck.setValue(false);
  } else if (curlCheck.isOver() == true) { 
    speedCheck.setValue(false);   
    vxCheck.setValue(false);   
    vyCheck.setValue(false);   
    curlCheck.setValue(true);    
    densityCheck.setValue(false);
  } else if (densityCheck.isOver() == true) { 
    speedCheck.setValue(false);   
    vxCheck.setValue(false);   
    vyCheck.setValue(false);   
    curlCheck.setValue(false);   
    densityCheck.setValue(true);
  }
}

byte plotType() {
  byte x = 0;
  if (speedCheck.b == true) x = 0;
  else if (vxCheck.b == true) x = 1;
  else if (vyCheck.b == true) x = 2;
  else if (curlCheck.b == true) x = 3;
  else if (densityCheck.b == true) x = 4;
  return x;
}
