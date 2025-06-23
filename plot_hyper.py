import pandas as pd
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
import re

# Load your CSV file
df = pd.read_csv("results_csv/aco_param_sweep.csv")

# Extract Ants, Iters, Evap from Algorithm
def extract_params(alg):
    match = re.search(r"Ants(\d+)_Iters(\d+)_Evap([0-9.]+)_iter", alg)
    return int(match.group(1)), int(match.group(2)), float(match.group(3))

df[['Ants', 'Iters', 'Evap']] = df['Algorithm'].apply(lambda x: pd.Series(extract_params(x)))

# Add GroupKey for each config + iteration group
df['GroupKey'] = df['Algorithm']

# Keep best row per group (minimum PowerConsumption)
best = df.loc[df.groupby('GroupKey')['PowerConsumption'].idxmin()].copy()

# Add SLA success %
best['SLA_SuccessPct'] = 100 - best['ViolationPct']

# Filter to keep only configurations with <= 20% violations
best = best[best['ViolationPct'] <= 20]
best = best[best['PowerConsumption'] < 220]  # Ensure SLA Success % is positive

# Create the combined plot
fig = plt.figure()
ax = fig.add_subplot(111, projection='3d')

# Normalize and enlarge marker sizes
min_ants = best['Ants'].min()
max_ants = best['Ants'].max()
best['Size'] = ((best['Ants'] - min_ants) / (max_ants - min_ants) * 200 + 50) * 1.5  # Scaled up

# Marker shape per Evap value
markers = {0.1: 'o', 0.2: 's'}
labels = {0.1: 'Evap 0.1 (circles)', 0.2: 'Evap 0.2 (squares)'}

# Plot each Evap subset
for evap_val in [0.1, 0.2]:
    subset = best[best['Evap'] == evap_val]
    sc = ax.scatter(
        subset['PowerConsumption'],
        subset['SLA_SuccessPct'],
        subset['TotalTimeTaken'],
        c=subset['Iters'],
        s=subset['Size'],
        cmap='plasma',
        alpha=0.5,
        marker=markers[evap_val],
        label=labels[evap_val]
    )

    # Add Ants label to each point
    for _, row in subset.iterrows():
        ax.text(row['PowerConsumption'], row['SLA_SuccessPct'], row['TotalTimeTaken'],
                f"{int(row['Ants'])}", size=8, zorder=1)

# Labels and formatting
ax.set_xlabel('Power Consumption')
ax.set_ylabel('SLA Success %')
ax.set_zlabel('Total Time Taken')
ax.set_title('ACO Results (≤20% SLA Violations) with Evap Shape Encoding')

# Colorbar and legend
cbar = fig.colorbar(sc, ax=ax, shrink=0.6, pad=0.1)
cbar.set_label('Iters')
ax.legend(loc='upper left', bbox_to_anchor=(1.05, 1))

plt.tight_layout()
plt.savefig("aco_3d_plot.png", dpi=300, bbox_inches='tight')
plt.show()
