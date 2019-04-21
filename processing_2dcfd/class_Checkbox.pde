class Checkbox {
  int x, y;
  String label;
  boolean b;
  boolean fullReset;

  Checkbox(int _x, int _y, String _label, boolean initValue, boolean _fullReset) {
    x = _x;
    y = _y+ydim*3;
    label = _label;
    b = initValue;
    fullReset = _fullReset;
  }

  void render() {
    fill(240);
    rect(x,y,100,20);
    fill(isOver()?128:64);
    rect(x, y, 20, 20);
    if (b) {
      stroke(240);
      line(x, y, x+20, y+20);
      line(x, y+20, x+20, y);
      noStroke();
    }
    textAlign(LEFT, CENTER);
    text(label, x+30, y+10);
  }

  void click() {
    if (isOver()) b=!b;
    if (fullReset) initTracers();
  }

  boolean isOver() {
    return(mouseX>x && mouseX<x+20 && mouseY>y && mouseY<y+20);
  }

  void setValue(boolean a) {
    b = a;
  }

  boolean getValue() {
    return b;
  }
}
