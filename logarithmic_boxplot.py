import pandas as pd
import matplotlib.pyplot as plt
from pathlib import Path

# Load all CSV files
input_dir = Path("results_csv/results_multiple_iterations_algorithm_smaller_aco")
csv_files = list(input_dir.glob("*.csv"))

# Collect all relevant data
stability_data = []

for file in csv_files:
    df = pd.read_csv(file)

    if df.empty or "ViolationPct" not in df.columns:
        continue

    # Extract algorithm base name and run id
    df["RunId"] = df["Algorithm"].str.extract(r"times_(\d+)")
    df["BaseAlgorithm"] = df["Algorithm"].str.extract(r"(^[^_]+)")

    # Filter for SLA < 10%
    df = df[df["ViolationPct"] < 10]

    if df.empty:
        continue

    for run_id, group in df.groupby("RunId"):
        if not group.empty:
            min_power = group["PowerConsumption"].min()
            base_name = group["BaseAlgorithm"].iloc[0]
            stability_data.append({
                "Algorithm": base_name,
                "PowerConsumption": min_power
            })

# Create DataFrame
stability_df = pd.DataFrame(stability_data)

# Ensure columns are correct
if not stability_df.empty and "Algorithm" in stability_df.columns:
    # Plot boxplot (log scale)
    plt.figure(figsize=(10, 6))
    stability_df.boxplot(by="Algorithm", column="PowerConsumption", grid=True)
    plt.yscale("log")
    plt.title("Power Consumption Stability per Algorithm (SLA < 10%)")
    plt.suptitle("")
    plt.xlabel("Algorithm")
    plt.ylabel("Power Consumption (W) [Log Scale]")
    plt.tight_layout()

    # Save
    output_dir = Path("plots/boxplots")
    output_dir.mkdir(parents=True, exist_ok=True)
    plot_path = output_dir / "boxplot_power_stability_log_smaller_aco.png"
    plt.savefig(plot_path)
    plt.close()

    print(f"✅ Boxplot saved to: {plot_path}")
else:
    print("⚠ No valid SLA-compliant data found for plotting.")
