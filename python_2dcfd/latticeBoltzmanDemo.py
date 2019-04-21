# LatticeBoltzmannDemo.py:  a two-dimensional lattice-Boltzmann "wind tunnel" simulation
# Uses numpy to speed up all array handling.
# Uses matplotlib to plot and animate the curl of the macroscopic velocity field.

# to-do make interactive

import numpy
import time
import matplotlib.pyplot
import matplotlib.animation

# Define constants:
height = 80							# lattice dimensions
width = 200
viscosity = 0.02					# fluid viscosity
omega = 1 / (3 * viscosity + 0.5)		# "relaxation" parameter
u0 = 0.1							# initial and in-flow speed
FOUR_NINTHS = 4.0 / 9.0					# abbreviations for lattice-Boltzmann weight factors
ONE_NINTH = 1.0 / 9.0
ONE_THIRTYSIXTH = 1.0 / 36.0

# precomputre a repeated term
u02 = u0 * u0
# Initialize all the arrays to steady rightward flow:
# particle densities along 9 directions
n0 = FOUR_NINTHS * (numpy.ones((height, width)) - 1.5 * u02)
nN = ONE_NINTH * (numpy.ones((height, width)) - 1.5 * u02)
nS = ONE_NINTH * (numpy.ones((height, width)) - 1.5 * u02)
nE = ONE_NINTH * (numpy.ones((height, width)) + 3 * u0 + 4.5 * u02 - 1.5 * u02)
nW = ONE_NINTH * (numpy.ones((height, width)) - 3 * u0 + 4.5 * u02 - 1.5 * u02)
nNE = ONE_THIRTYSIXTH * (numpy.ones((height, width)) + 3 * u0 + 4.5 * u02 - 1.5 * u02)
nSE = ONE_THIRTYSIXTH * (numpy.ones((height, width)) + 3 * u0 + 4.5 * u02 - 1.5 * u02)
nNW = ONE_THIRTYSIXTH * (numpy.ones((height, width)) - 3 * u0 + 4.5 * u02 - 1.5 * u02)
nSW = ONE_THIRTYSIXTH * (numpy.ones((height, width)) - 3 * u0 + 4.5 * u02 - 1.5 * u02)
rho = n0 + nN + nS + nE + nW + nNE + nSE + nNW + nSW		# macroscopic density
ux = (nE + nNE + nSE - nW - nNW - nSW) / rho				# macroscopic x velocity
uy = (nN + nNE + nNW - nS - nSE - nSW) / rho				# macroscopic y velocity

# Initialize barriers:
# True wherever there's a barrier
barrier = numpy.zeros((height, width), bool)
barrier[40][32:48] = True	# simple linear barrier
barrierN = numpy.roll(barrier,  1, axis=0)					# sites just north of barriers
barrierS = numpy.roll(barrier, -1, axis=0)					# sites just south of barriers
barrierE = numpy.roll(barrier,  1, axis=1)					# etc.
barrierW = numpy.roll(barrier, -1, axis=1)
barrierNE = numpy.roll(barrierN,  1, axis=1)
barrierNW = numpy.roll(barrierN, -1, axis=1)
barrierSE = numpy.roll(barrierS,  1, axis=1)
barrierSW = numpy.roll(barrierS, -1, axis=1)

# Move all particles by one step along their directions of motion (pbc):
def stream():
    global nN, nS, nE, nW, nNE, nNW, nSE, nSW
    # axis 0 is north-south; + direction is north
    nN = numpy.roll(nN,   1, axis=0)
    nNE = numpy.roll(nNE,  1, axis=0)
    nNW = numpy.roll(nNW,  1, axis=0)
    nS = numpy.roll(nS,  -1, axis=0)
    nSE = numpy.roll(nSE, -1, axis=0)
    nSW = numpy.roll(nSW, -1, axis=0)
    # axis 1 is east-west; + direction is east
    nE = numpy.roll(nE,   1, axis=1)
    nNE = numpy.roll(nNE,  1, axis=1)
    nSE = numpy.roll(nSE,  1, axis=1)
    nW = numpy.roll(nW,  -1, axis=1)
    nNW = numpy.roll(nNW, -1, axis=1)
    nSW = numpy.roll(nSW, -1, axis=1)
    # Use tricky boolean arrays to handle barrier collisions (bounce-back):
    nN[barrierN] = nS[barrier]
    nS[barrierS] = nN[barrier]
    nE[barrierE] = nW[barrier]
    nW[barrierW] = nE[barrier]
    nNE[barrierNE] = nSW[barrier]
    nNW[barrierNW] = nSE[barrier]
    nSE[barrierSE] = nNW[barrier]
    nSW[barrierSW] = nNE[barrier]

# Collide particles within each cell to redistribute velocities (could be optimized a little more):
def collide():
    global rho, ux, uy, n0, nN, nS, nE, nW, nNE, nNW, nSE, nSW
    rho = n0 + nN + nS + nE + nW + nNE + nSE + nNW + nSW
    ux = (nE + nNE + nSE - nW - nNW - nSW) / rho
    uy = (nN + nNE + nNW - nS - nSE - nSW) / rho
    ux2 = ux * ux				# pre-compute terms used repeatedly...
    uy2 = uy * uy
    u2 = ux2 + uy2
    #u02 = u0 * u0
    omu215 = 1 - 1.5 * u2			# "one minus u2 times 1.5"
    uxuy = ux * uy
    coeff = omega * rho
    n0 = (1 - omega) * n0 + coeff * FOUR_NINTHS * omu215
    nN = (1 - omega) * nN + coeff * ONE_NINTH * (omu215 + 3 * uy + 4.5 * uy2)
    nS = (1 - omega) * nS + coeff * ONE_NINTH * (omu215 - 3 * uy + 4.5 * uy2)
    nE = (1 - omega) * nE + coeff * ONE_NINTH * (omu215 + 3 * ux + 4.5 * ux2)
    nW = (1 - omega) * nW + coeff * ONE_NINTH * (omu215 - 3 * ux + 4.5 * ux2)
    nNE = (1 - omega) * nNE + coeff * ONE_THIRTYSIXTH * (omu215 + 3 * (ux + uy) + 4.5 * (u2 + 2 * uxuy))
    nNW = (1 - omega) * nNW + coeff * ONE_THIRTYSIXTH * (omu215 + 3 * (-ux + uy) + 4.5 * (u2 - 2 * uxuy))
    nSE = (1 - omega) * nSE + coeff * ONE_THIRTYSIXTH * (omu215 + 3 * (ux - uy) + 4.5 * (u2 - 2 * uxuy))
    nSW = (1 - omega) * nSW + coeff * ONE_THIRTYSIXTH * (omu215 + 3 * (-ux - uy) + 4.5 * (u2 + 2 * uxuy))
    # Force steady rightward flow at ends (no need to set 0, N, and S components):
    nE[:, 0] = ONE_NINTH * (1 + 3 * u0 + 4.5 * u02 - 1.5 * u02)
    nW[:, 0] = ONE_NINTH * (1 - 3 * u0 + 4.5 * u02 - 1.5 * u02)
    nNE[:, 0] = ONE_THIRTYSIXTH * (1 + 3 * u0 + 4.5 * u02 - 1.5 * u02)
    nSE[:, 0] = ONE_THIRTYSIXTH * (1 + 3 * u0 + 4.5 * u02 - 1.5 * u02)
    nNW[:, 0] = ONE_THIRTYSIXTH * (1 - 3 * u0 + 4.5 * u02 - 1.5 * u02)
    nSW[:, 0] = ONE_THIRTYSIXTH * (1 - 3 * u0 + 4.5 * u02 - 1.5 * u02)

# Compute curl of the macroscopic velocity field:
def curl(ux, uy):
    return (numpy.roll(uy, -1, axis=1) - numpy.roll(uy, 1, axis=1) - numpy.roll(ux, -1, axis=0) + numpy.roll(ux, 1, axis=0))

# Here comes the graphics and animation...
theFig = matplotlib.pyplot.figure(figsize=(8, 3))
fluidImage = matplotlib.pyplot.imshow(curl(ux, uy), origin='lower', norm=matplotlib.pyplot.Normalize(-.1, .1), cmap=matplotlib.pyplot.get_cmap('jet'), interpolation='none')
# See http://www.loria.fr/~rougier/teaching/matplotlib/#colormaps for other cmap options
bImageArray = numpy.zeros((height, width, 4), numpy.uint8)  # an RGBA image
bImageArray[barrier, 3] = 255								# set alpha=255 only at barrier sites
barrierImage = matplotlib.pyplot.imshow(
    bImageArray, origin='lower', interpolation='none')

# Function called for each successive animation frame:
startTime = time.clock()
def nextFrame(arg):							# (arg is the frame number, which we don't need)
    global startTime
    for step in range(20):					# adjust number of steps for smooth animation
        stream()
        collide()
    fluidImage.set_array(curl(ux, uy))
    return (fluidImage, barrierImage)		# return the figure elements to redraw

animate = matplotlib.animation.FuncAnimation(theFig, nextFrame, interval=1, blit=True)
matplotlib.pyplot.show()
