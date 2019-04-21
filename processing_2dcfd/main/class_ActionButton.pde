class ActionButton {
  int x, y;
  int wide, high;
  boolean b;
  String label;

  ActionButton(int _x, int _y, String _label, int _width, int _height) {
    x = _x;
    y = _y+ydim*3;
    wide = _width;
    high = _height;
    label = _label;
  }

  void render() {
    fill(isOver()?128:64);
    rect(x, y, wide, high);
    textAlign(CENTER, CENTER);
    fill(240);
    text(label, x+wide/2, y+high/2);
  }

  boolean isOver() {
    return(mouseX>x && mouseX<x+wide && mouseY>y && mouseY<y+high);
  }
}
