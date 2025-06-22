import pandas as pd
import matplotlib.pyplot as plt
from pathlib import Path

# Load the ACO hyperparameter sweep CSV
csv_path = Path("results_csv/aco_param_sweep.csv")
df = pd.read_csv(csv_path)

# Compute SLA success and energy saved (%)
df["SLA_Success"] = 100 - df["ViolationPct"]
max_power = df["PowerConsumption"].max()
df["EnergySavedPct"] = (max_power - df["PowerConsumption"]) / max_power * 100

# From each hyperparameter config (Algorithm), pick the row with max energy saved
best_per_config = df.loc[df.groupby("Algorithm")["EnergySavedPct"].idxmax()]

# Plot: Energy Saved vs SLA Success
plt.figure(figsize=(12, 7))
scatter = plt.scatter(
    best_per_config["EnergySavedPct"],
    best_per_config["SLA_Success"],
    s=100,
    alpha=0.8,
    edgecolor="black"
)

# Annotate with hyperparameter config
for _, row in best_per_config.iterrows():
    plt.annotate(
        row["Algorithm"],
        (row["EnergySavedPct"], row["SLA_Success"]),
        fontsize=8,
        xytext=(5, 5),
        textcoords='offset points'
    )

# Labels and styling
plt.xlabel("Max Energy Saved per Config (%)")
plt.ylabel("SLA Success (%)")
plt.title("Best Energy-Saving Configurations vs SLA Success (ACO)")
plt.grid(True)
plt.tight_layout()

# Save the plot
out_dir = Path("plots/aco_hyperparam_analysis")
out_dir.mkdir(parents=True, exist_ok=True)
plot_path = out_dir / "best_energy_per_config_vs_sla_success.png"
plt.savefig(plot_path)
plt.close()

plot_path
