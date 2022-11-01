
# %%
import circlify
import matplotlib.pyplot as plt
import matplotlib.patches as patches
import pandas as pd
import numpy as np
import matplotlib.animation as animation
import json
import time


# %%
fig, ax = plt.subplots(figsize=(10,10))

def animate(i):
    f = open('/Users/daniel.burke/Downloads/test_data.json')
    test_data = json.load(f)
    circles = circlify.circlify(
        test_data, 
        show_enclosure=False, 
        target_enclosure=circlify.Circle(x=0, y=0, r=1)
    )

    #ax.clear()
    #fig.clear(True)
    
    # Title
    ax.set_title('Passenger Planes by Country Airspace')

    # Remove axes
    ax.axis('off')

    # Find axis boundaries
    lim = max(
        max(
            abs(circle.x) + circle.r,
            abs(circle.y) + circle.r,
        )
        for circle in circles
    )
    plt.xlim(-lim, lim)
    plt.ylim(-lim, lim)

    # list of labels
    #labels = data('id')

    # print circles
    ann_name_list = []
    ann_size_list = []
    for circle in circles:
        x, y, r = circle

        ax.add_patch(patches.Circle((x, y), r, alpha=0.2, linewidth=2, facecolor='white', edgecolor="black"))
        id_label = circle.ex["id"]
        datum_label=circle.ex["datum"]
        
        ann_name = plt.annotate(id_label, (x,y ) ,va='center', ha='center', fontsize=r*30, bbox=dict(facecolor='white', edgecolor='black', boxstyle='round', pad=.5))
        ann_name_list.append(ann_name)
        
        ann_size = plt.annotate(datum_label, (x,y-((1/2)*r) ) ,va='center', ha='center', fontsize=r*30, bbox=dict(facecolor='white', edgecolor='black', boxstyle='round', pad=.5))
        ann_size_list.append(ann_size)
        #plt.pause(3)
        #plt.draw
    plt.pause(10)
    [p.remove() for p in reversed(ax.patches)]
    [n.remove() for n in reversed(ann_name_list)]
    [s.remove() for s in reversed(ann_size_list)]


ani = animation.FuncAnimation(fig, animate, interval=1000)
plt.show()


for cl in db.list_collections():
    print(cl)

for db in client.list_databases():
    print(db)
        