class Scroller {
  int x, y;
  float min, max;
  String label;
  float value;

  Scroller(int _x, int _y, float _min, float _max, String _label, float initValue) {
    x = _x;
    y = _y;
    min = _min;
    max = _max;
    label = _label;
    value = initValue;
  }

  void render() {
    fill(240);
    rect(x, y+ydim*3, 340, 20);
    fill(64);
    textAlign(LEFT, CENTER);
    text(label + nf(value, 1, 2), x, y+ydim*3+10);
    rect(x+120, y+ydim*3, 200, 20);
    fill(128);
    ellipse(map(value, min, max, x+120, x+320), y+10+ydim*3, 20, 20);
  }

  void update() {
    if (mouseX>=x+120 && mouseX<=x+320 && mouseY>=y+ydim*3 && mouseY<=y+ydim*3+20)
      value = map(mouseX, x+120, x+320, min, max);
  }
}
